package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.AuctionRoomRequest;
import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.DTO.Response.AuctionRoomLiveDTO;
import com.auctionaa.backend.DTO.Response.MemberResponse;
import com.auctionaa.backend.DTO.Response.RoomCompleteDetailDTO;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.AuctionSessionRepository;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuctionRoomService {

    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.10");
    private static final int SESSION_STATUS_RUNNING = 1;

    @Autowired
    private AuctionRoomRepository auctionRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenericSearchService genericSearchService;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    public AuctionRoom getRoomById(String roomId){
        return auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found!"));
    }
    public List<AuctionRoom> getByOwnerEmail(String email, int page, int size) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());

        Page<AuctionRoom> pageResult =
                auctionRoomRepository.findByMemberIdsContaining(user.getId(), pageable);

        List<AuctionRoom> rooms = pageResult.getContent();

        initializeDepositForRooms(rooms);

        return rooms;
    }




    public List<AuctionRoom> getAllAuctionRoom() {
        List<AuctionRoom> rooms = auctionRoomRepository.findAllByOrderByCreatedAtDesc();
        initializeDepositForRooms(rooms);
        return rooms;
    }

    // Hàm thêm auction room mới
    public AuctionRoom createAuctionRoom(AuctionRoomRequest req, String email) {
        User creator = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AuctionRoom room = new AuctionRoom();
        room.generateId();
        room.setRoomName(req.getRoomName());
        room.setDescription(req.getDescription());
        room.setImageAuctionRoom(req.getImageAuctionRoom());
        room.setType(req.getType());
        room.setStatus(req.getStatus());
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        room.setDepositAmount(BigDecimal.ZERO);

        // admin = người tạo
        room.setAdminId(creator.getId());

        // memberIds ban đầu = danh sách được request + cả admin
        List<String> memberIds = req.getMemberIds() != null ? req.getMemberIds() : new ArrayList<>();
        if (!memberIds.contains(creator.getId())) {
            memberIds.add(creator.getId());
        }
        room.setMemberIds(memberIds);

        return auctionRoomRepository.save(room);
    }

    public List<AuctionRoomLiveDTO> getTop6HotAuctionRooms() {
        List<AuctionRoomLiveDTO> rooms = auctionRoomRepository.findTop6ByMembersCount();
        initializeDepositForLiveRooms(rooms);
        return rooms;
    }

    public List<AuctionRoomLiveDTO> getRoomsWithLivePrices(int page, int size) {
        long skip = (long) page * size;
        long limit = size;

        List<AuctionRoomLiveDTO> rooms =
                auctionRoomRepository.findRoomsWithLivePrices(
                        SESSION_STATUS_RUNNING,
                        skip,
                        limit
                );

        initializeDepositForLiveRooms(rooms);
        return rooms;
    }

    /**
     * Lấy danh sách phòng đấu giá đang diễn ra (status = 1) với phân trang
     */
    public List<AuctionRoom> getOngoingRooms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<AuctionRoom> pageResult = auctionRoomRepository.findByStatus(1, pageable);
        List<AuctionRoom> rooms = pageResult.getContent();
        initializeDepositForRooms(rooms);
        return rooms;
    }

    /**
     * Lấy danh sách phòng đấu giá sắp diễn ra (status = 2) với phân trang
     */
    public List<AuctionRoom> getUpcomingRooms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<AuctionRoom> pageResult = auctionRoomRepository.findByStatus(2, pageable);
        List<AuctionRoom> rooms = pageResult.getContent();
        initializeDepositForRooms(rooms);
        return rooms;
    }


    /**
     * Tìm kiếm và lọc auction room của user hiện tại theo các tiêu chí:
     * - Tìm kiếm theo ID (exact match)
     * - Tìm kiếm theo tên phòng (partial match, case-insensitive)
     * - Lọc theo thể loại (type)
     * - Lọc theo ngày tạo (dateFrom, dateTo)
     * - Filter theo adminId hoặc memberIds (user là admin hoặc member)
     * 
     * Logic: Nếu có cả ID và name, tìm theo ID HOẶC name (OR logic)
     */
    public List<AuctionRoom> searchAndFilter(BaseSearchRequest request, String userId) {
        List<AuctionRoom> rooms;
        
        // Nếu có cả ID và name, tìm theo cả hai (OR logic)
        boolean hasId = request.getId() != null && !request.getId().trim().isEmpty();
        boolean hasName = request.getName() != null && !request.getName().trim().isEmpty();
        
        if (hasId && hasName) {
            // Tìm theo ID
            List<AuctionRoom> byId = auctionRoomRepository.findById(request.getId().trim())
                    .map(List::of)
                    .orElse(new ArrayList<>());
            
            // Tìm theo name
            List<AuctionRoom> byName = auctionRoomRepository.findByRoomNameContainingIgnoreCase(request.getName().trim());
            
            // Kết hợp kết quả (loại bỏ duplicate)
            Set<String> seenIds = new HashSet<>();
            rooms = new ArrayList<>();
            for (AuctionRoom room : byId) {
                if (!seenIds.contains(room.getId())) {
                    rooms.add(room);
                    seenIds.add(room.getId());
                }
            }
            for (AuctionRoom room : byName) {
                if (!seenIds.contains(room.getId())) {
                    rooms.add(room);
                    seenIds.add(room.getId());
                }
            }
            
            // Áp dụng các filter khác (type, dateFrom, dateTo)
            rooms = applyAdditionalFilters(rooms, request);
        } else {
            // Sử dụng GenericSearchService cho các trường hợp khác
            rooms = genericSearchService.searchAndFilter(
                    request,
                    AuctionRoom.class,
                    "_id", // idField
                    "roomName", // nameField
                    "type", // typeField
                    "createdAt", // dateField
                    null, // userIdField (không dùng vì cần check cả adminId và memberIds)
                    null // userId (sẽ filter sau)
            );
        }

        // Filter theo userId: user là admin hoặc là member
        if (userId != null && !userId.isEmpty()) {
            rooms = rooms.stream()
                    .filter(room -> userId.equals(room.getAdminId())
                            || (room.getMemberIds() != null && room.getMemberIds().contains(userId)))
                    .toList();
        }

        // Sắp xếp theo createdAt DESC (mới nhất trước)
        rooms = rooms.stream()
                .sorted((r1, r2) -> {
                    LocalDateTime c1 = r1.getCreatedAt();
                    LocalDateTime c2 = r2.getCreatedAt();
                    if (c1 == null && c2 == null) return 0;
                    if (c1 == null) return 1;
                    if (c2 == null) return -1;
                    return c2.compareTo(c1); // DESC
                })
                .collect(java.util.stream.Collectors.toList());

        initializeDepositForRooms(rooms);
        return rooms;
    }
    
    /**
     * Áp dụng các filter bổ sung (type, dateFrom, dateTo) cho danh sách rooms
     */
    private List<AuctionRoom> applyAdditionalFilters(List<AuctionRoom> rooms, BaseSearchRequest request) {
        return rooms.stream()
                .filter(room -> {
                    // Filter theo type
                    if (request.getType() != null && !request.getType().trim().isEmpty()) {
                        if (room.getType() == null || !room.getType().equals(request.getType().trim())) {
                            return false;
                        }
                    }
                    
                    // Filter theo dateFrom
                    if (request.getDateFrom() != null && room.getCreatedAt() != null) {
                        if (room.getCreatedAt().toLocalDate().isBefore(request.getDateFrom())) {
                            return false;
                        }
                    }
                    
                    // Filter theo dateTo
                    if (request.getDateTo() != null && room.getCreatedAt() != null) {
                        if (room.getCreatedAt().toLocalDate().isAfter(request.getDateTo())) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .toList();
    }

    /**
     * Tìm kiếm auction room công khai (không filter theo userId)
     * Tìm kiếm theo ID hoặc tên phòng, trả về toàn bộ thông tin
     * 
     * @param request BaseSearchRequest với id hoặc name
     * @return Danh sách auction room với đầy đủ thông tin
     */
    public List<AuctionRoom> searchAuctionRoomsPublic(BaseSearchRequest request) {
        if (request == null) {
            request = new BaseSearchRequest();
        }
        
        List<AuctionRoom> rooms;
        
        // Nếu có cả ID và name, tìm theo cả hai (OR logic)
        boolean hasId = request.getId() != null && !request.getId().trim().isEmpty();
        boolean hasName = request.getName() != null && !request.getName().trim().isEmpty();
        
        if (hasId && hasName) {
            // Tìm theo ID
            List<AuctionRoom> byId = auctionRoomRepository.findById(request.getId().trim())
                    .map(List::of)
                    .orElse(new ArrayList<>());
            
            // Tìm theo name
            List<AuctionRoom> byName = auctionRoomRepository.findByRoomNameContainingIgnoreCase(request.getName().trim());
            
            // Kết hợp kết quả (loại bỏ duplicate)
            Set<String> seenIds = new HashSet<>();
            rooms = new ArrayList<>();
            for (AuctionRoom room : byId) {
                if (!seenIds.contains(room.getId())) {
                    rooms.add(room);
                    seenIds.add(room.getId());
                }
            }
            for (AuctionRoom room : byName) {
                if (!seenIds.contains(room.getId())) {
                    rooms.add(room);
                    seenIds.add(room.getId());
                }
            }
            
            // Áp dụng các filter khác (type, dateFrom, dateTo)
            rooms = applyAdditionalFilters(rooms, request);
        } else if (hasId) {
            // Chỉ tìm theo ID
            rooms = auctionRoomRepository.findById(request.getId().trim())
                    .map(List::of)
                    .orElse(new ArrayList<>());
        } else if (hasName) {
            // Chỉ tìm theo name
            rooms = auctionRoomRepository.findByRoomNameContainingIgnoreCase(request.getName().trim());
            // Áp dụng các filter khác
            rooms = applyAdditionalFilters(rooms, request);
        } else {
            // Không có điều kiện tìm kiếm, trả về tất cả (sắp xếp theo createdAt DESC)
            rooms = auctionRoomRepository.findAllByOrderByCreatedAtDesc();
            // Áp dụng các filter khác
            rooms = applyAdditionalFilters(rooms, request);
        }
        
        // Khởi tạo deposit amount cho các rooms
        initializeDepositForRooms(rooms);
        
        return rooms;
    }

    public BigDecimal recomputeAndPersistDeposit(String roomId) {
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction room not found"));
        BigDecimal deposit = calculateDepositAmount(roomId);
        room.setDepositAmount(deposit);
        auctionRoomRepository.save(room);
        return deposit;
    }

    private BigDecimal calculateDepositAmount(String roomId) {
        if (roomId == null) {
            return BigDecimal.ZERO;
        }
        return auctionSessionRepository
                .findFirstByAuctionRoomIdOrderByStartingPriceDesc(roomId)
                .map(AuctionSession::getStartingPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .map(price -> price.multiply(DEPOSIT_RATE))
                .orElse(BigDecimal.ZERO);
    }

    private void initializeDepositForRooms(List<AuctionRoom> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        for (AuctionRoom room : rooms) {
            if (room == null || room.getId() == null) {
                continue;
            }
            BigDecimal deposit = calculateDepositAmount(room.getId());
            if (room.getDepositAmount() == null || room.getDepositAmount().compareTo(deposit) != 0) {
                room.setDepositAmount(deposit);
                auctionRoomRepository.save(room);
            }
        }
    }

    private void initializeDepositForLiveRooms(List<AuctionRoomLiveDTO> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        for (AuctionRoomLiveDTO dto : rooms) {
            if (dto == null || dto.getId() == null) {
                continue;
            }
            BigDecimal deposit = calculateDepositAmount(dto.getId());
            dto.setDepositAmount(deposit);
            auctionRoomRepository.findById(dto.getId()).ifPresent(room -> {
                if (room.getDepositAmount() == null || room.getDepositAmount().compareTo(deposit) != 0) {
                    room.setDepositAmount(deposit);
                    auctionRoomRepository.save(room);
                }
            });
        }
    }

    /**
     * Lấy danh sách member của một phòng đấu giá
     * Bao gồm cả admin và các member trong memberIds
     *
     * @param roomId ID của phòng đấu giá
     * @return Danh sách member với id, username, avt
     */
    public List<MemberResponse> getRoomMembers(String roomId) {
        // 1. Tìm phòng đấu giá
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Auction room not found"));

        // 2. Tạo danh sách tất cả userIds (adminId + memberIds)
        List<String> userIds = new ArrayList<>();

        // Thêm adminId nếu có
        if (room.getAdminId() != null && !room.getAdminId().isEmpty()) {
            userIds.add(room.getAdminId());
        }

        // Thêm memberIds nếu có
        if (room.getMemberIds() != null && !room.getMemberIds().isEmpty()) {
            for (String memberId : room.getMemberIds()) {
                // Tránh duplicate nếu adminId cũng có trong memberIds
                if (!userIds.contains(memberId)) {
                    userIds.add(memberId);
                }
            }
        }

        // 3. Nếu không có member nào, trả về empty list
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. Query tất cả users theo userIds
        List<User> users = userRepository.findAllById(userIds);

        // 5. Map sang MemberResponse
        return users.stream()
                .map(user -> new MemberResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getAvt()))
                .toList();
    }

    /**
     * Lấy tất cả thông tin của phòng đấu giá bao gồm:
     * - Thông tin phòng (AuctionRoom)
     * - Tất cả sessions trong phòng (AuctionSession)
     * - Thông tin tác phẩm (Artwork) của mỗi session
     *
     * @param roomId ID của phòng đấu giá
     * @return RoomCompleteDetailDTO chứa đầy đủ thông tin
     */
    public RoomCompleteDetailDTO getRoomCompleteDetail(String roomId) {
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        List<AuctionSession> sessions = auctionSessionRepository.findByAuctionRoomId(roomId);

        List<String> artworkIds = sessions.stream()
                .map(AuctionSession::getArtworkId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        List<Artwork> artworks = artworkIds.isEmpty()
                ? new ArrayList<>()
                : artworkRepository.findAllById(artworkIds);

        Map<String, Artwork> artworkMap = artworks.stream()
                .collect(java.util.stream.Collectors.toMap(Artwork::getId, a -> a));

        // ===== NEW: gom ownerIds từ artworks =====
        List<String> ownerIds = artworks.stream()
                .map(Artwork::getOwnerId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        Map<String, String> ownerNameMap = ownerIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : userRepository.findAllById(ownerIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        User::getId,
                        User::getUsername,   // "name" hiển thị hiện tại = username
                        (a, b) -> a
                ));
        List<RoomCompleteDetailDTO.SessionWithArtworkDTO> sessionWithArtworks = sessions.stream()
                .map(session -> {
                    Artwork artwork = artworkMap.get(session.getArtworkId());
                    String ownerName = null;

                    if (artwork != null && artwork.getOwnerId() != null) {
                        ownerName = ownerNameMap.get(artwork.getOwnerId());
                    }

                    return new RoomCompleteDetailDTO.SessionWithArtworkDTO(session, artwork, ownerName);
                })
                .toList();

        return new RoomCompleteDetailDTO(room, sessionWithArtworks);
    }
}
