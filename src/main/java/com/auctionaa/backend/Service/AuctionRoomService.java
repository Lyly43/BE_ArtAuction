package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.AuctionRoomRequest;
import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.DTO.Response.AuctionRoomLiveDTO;
import com.auctionaa.backend.DTO.Response.MemberResponse;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.AuctionSessionRepository;
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
import java.util.List;

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

    public AuctionRoom getRoomById(String roomId){
        return auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found!"));
    }
    public List<AuctionRoom> getByOwnerEmail(String email, int page, int size) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);

        Page<AuctionRoom> pageResult =
                auctionRoomRepository.findByMemberIdsContaining(user.getId(), pageable);

        List<AuctionRoom> rooms = pageResult.getContent();

        initializeDepositForRooms(rooms);

        return rooms;
    }




    public List<AuctionRoom> getAllAuctionRoom() {
        List<AuctionRoom> rooms = auctionRoomRepository.findAll();
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
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionRoom> pageResult = auctionRoomRepository.findByStatus(1, pageable);
        List<AuctionRoom> rooms = pageResult.getContent();
        initializeDepositForRooms(rooms);
        return rooms;
    }

    /**
     * Lấy danh sách phòng đấu giá sắp diễn ra (status = 2) với phân trang
     */
    public List<AuctionRoom> getUpcomingRooms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
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
     */
    public List<AuctionRoom> searchAndFilter(BaseSearchRequest request, String userId) {
        List<AuctionRoom> rooms = genericSearchService.searchAndFilter(
                request,
                AuctionRoom.class,
                "_id", // idField
                "roomName", // nameField
                "type", // typeField
                "createdAt", // dateField
                null, // userIdField (không dùng vì cần check cả adminId và memberIds)
                null // userId (sẽ filter sau)
        );

        // Filter theo userId: user là admin hoặc là member
        if (userId != null && !userId.isEmpty()) {
            rooms = rooms.stream()
                    .filter(room -> userId.equals(room.getAdminId())
                            || (room.getMemberIds() != null && room.getMemberIds().contains(userId)))
                    .toList();
        }

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
}
