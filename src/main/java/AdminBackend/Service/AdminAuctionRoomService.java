package AdminBackend.Service;

import AdminBackend.DTO.Request.AddAuctionRoomRequest;
import AdminBackend.DTO.Request.ArtworkPriceSetting;
import AdminBackend.DTO.Request.AuctionRoomFilterRequest;
import AdminBackend.DTO.Request.CreateAuctionRoomCompleteRequest;
import AdminBackend.DTO.Request.UpdateAuctionRoomRequest;
import AdminBackend.DTO.Response.AdminAuctionRoomResponse;
import AdminBackend.DTO.Response.AdminBasicResponse;
import AdminBackend.DTO.Response.ArtworkForSelectionResponse;
import AdminBackend.DTO.Response.AuctionRoomDetailResponse;
import AdminBackend.DTO.Response.AuctionRoomStatisticsResponse;
import AdminBackend.DTO.Response.MonthlyComparisonResponse;
import AdminBackend.DTO.Response.UpdateResponse;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Entity.User;
import AdminBackend.Repository.AdminRepository;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.AuctionSessionRepository;
import com.auctionaa.backend.Repository.InvoiceRepository;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.CloudinaryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminAuctionRoomService {

    @Autowired
    private AuctionRoomRepository auctionRoomRepository;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private MonthlyStatisticsService monthlyStatisticsService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Admin thêm phòng đấu giá mới
     */
    public ResponseEntity<AdminBasicResponse<AdminAuctionRoomResponse>> addAuctionRoom(AddAuctionRoomRequest request) {
        if (!StringUtils.hasText(request.getRoomName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "roomName is required", null));
        }

        if (request.getStartedAt() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "startedAt is required", null));
        }

        if (!request.getStartedAt().isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "startedAt must be in the future", null));
        }

        AuctionRoom room = new AuctionRoom();
        room.generateId();
        room.setRoomName(request.getRoomName());
        room.setDescription(request.getDescription());
        room.setType(request.getType());
        room.setImageAuctionRoom(request.getImageAuctionRoom());
        room.setStartedAt(request.getStartedAt());
        room.setStoppedAt(request.getStoppedAt());
        room.setEstimatedEndTime(request.getEstimatedEndTime());
        room.setViewCount(0);
        room.setStatus(determineStatus(request.getStartedAt(), request.getStoppedAt()));
        room.setMemberIds(new ArrayList<>());
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        AuctionRoom saved = auctionRoomRepository.save(room);
        AdminAuctionRoomResponse response = mapToResponse(saved, true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminBasicResponse<>(1, "Auction room created successfully", response));
    }

    /**
     * Lấy tất cả phòng đấu giá (sắp xếp theo createdAt DESC - mới nhất trước)
     */
    public ResponseEntity<List<AdminAuctionRoomResponse>> getAllAuctionRooms() {
        List<AuctionRoom> rooms = auctionRoomRepository.findAllByOrderByCreatedAtDesc();
        List<AdminAuctionRoomResponse> responses = rooms.stream()
                .map(this::mapToResponseAndUpdateStatusIfNeeded)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Tìm kiếm theo room id hoặc roomName (sắp xếp theo createdAt DESC - mới nhất trước)
     */
    public ResponseEntity<List<AdminAuctionRoomResponse>> searchAuctionRooms(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllAuctionRooms();
        }

        String trimmed = searchTerm.trim();
        Set<AuctionRoom> resultSet = new LinkedHashSet<>();

        auctionRoomRepository.findById(trimmed).ifPresent(resultSet::add);

        List<AuctionRoom> byName = auctionRoomRepository.findByRoomNameContainingIgnoreCase(trimmed);
        resultSet.addAll(byName);

        // Sắp xếp theo createdAt DESC (mới nhất trước)
        List<AuctionRoom> sortedRooms = resultSet.stream()
                .sorted(Comparator.comparing(AuctionRoom::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .collect(Collectors.toList());

        List<AdminAuctionRoomResponse> responses = sortedRooms.stream()
                .map(this::mapToResponseAndUpdateStatusIfNeeded)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Lọc phòng đấu giá theo các tiêu chí
     * Tất cả các trường filter đều optional (null = bỏ qua filter đó)
     */
    public ResponseEntity<List<AdminAuctionRoomResponse>> filterAuctionRooms(AuctionRoomFilterRequest request) {
        // Nếu request null, trả về tất cả auction rooms
        if (request == null) {
            return getAllAuctionRooms();
        }
        
        List<AuctionRoom> allRooms = auctionRoomRepository.findAll();
        
        List<AuctionRoom> filteredRooms = allRooms.stream()
                .filter(room -> {
                    // Filter by statuses (null hoặc empty = bỏ qua filter)
                    if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                        // Cập nhật status nếu cần (dựa trên startedAt/stoppedAt)
                        updateStatusIfNeeded(room);
                        int roomStatus = room.getStatus();
                        if (!request.getStatuses().contains(roomStatus)) {
                            return false;
                        }
                    }
                    
                    // Filter by startTime range (lọc theo startedAt)
                    if (request.getStartTimeFrom() != null) {
                        if (room.getStartedAt() == null || 
                            room.getStartedAt().isBefore(request.getStartTimeFrom())) {
                            return false;
                        }
                    }
                    if (request.getStartTimeTo() != null) {
                        if (room.getStartedAt() == null || 
                            room.getStartedAt().isAfter(request.getStartTimeTo())) {
                            return false;
                        }
                    }
                    
                    // Filter by endTime range (lọc theo stoppedAt)
                    if (request.getEndTimeFrom() != null) {
                        if (room.getStoppedAt() == null || 
                            room.getStoppedAt().isBefore(request.getEndTimeFrom())) {
                            return false;
                        }
                    }
                    if (request.getEndTimeTo() != null) {
                        if (room.getStoppedAt() == null || 
                            room.getStoppedAt().isAfter(request.getEndTimeTo())) {
                            return false;
                        }
                    }
                    
                    // Filter by participants range (lọc theo totalMembers = số lượng memberIds)
                    if (request.getParticipantsRange() != null && 
                        !request.getParticipantsRange().trim().isEmpty() &&
                        !request.getParticipantsRange().trim().equalsIgnoreCase("all")) {
                        int totalMembers = (room.getMemberIds() != null) ? room.getMemberIds().size() : 0;
                        String range = request.getParticipantsRange().trim().toLowerCase();
                        
                        switch (range) {
                            case "<10":
                                if (totalMembers >= 10) {
                                    return false;
                                }
                                break;
                            case "10-50":
                                if (totalMembers < 10 || totalMembers > 50) {
                                    return false;
                                }
                                break;
                            case ">50":
                                if (totalMembers <= 50) {
                                    return false;
                                }
                                break;
                            default:
                                // Nếu giá trị không hợp lệ, bỏ qua filter này
                                break;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        // Sắp xếp theo createdAt DESC (mới nhất trước)
        filteredRooms.sort(Comparator.comparing(AuctionRoom::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
        
        List<AdminAuctionRoomResponse> responses = filteredRooms.stream()
                .map(this::mapToResponseAndUpdateStatusIfNeeded)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Update phòng đấu giá
     */
    public ResponseEntity<?> updateAuctionRoom(String roomId, UpdateAuctionRoomRequest request) {
        Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            UpdateResponse<Object> resp = new UpdateResponse<>(0,
                    "Auction room not found with ID: " + roomId, null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }

        AuctionRoom room = roomOpt.get();

        if (StringUtils.hasText(request.getRoomName())) {
            room.setRoomName(request.getRoomName());
        }
        if (request.getDescription() != null) {
            room.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            room.setType(request.getType());
        }
        if (request.getImageAuctionRoom() != null) {
            room.setImageAuctionRoom(request.getImageAuctionRoom());
        }
        if (request.getViewCount() != null) {
            room.setViewCount(request.getViewCount());
        }
        if (request.getStartedAt() != null) {
            if (!request.getStartedAt().isAfter(LocalDateTime.now())) {
                UpdateResponse<Object> resp = new UpdateResponse<>(0,
                        "startedAt must be in the future", null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
            }
            room.setStartedAt(request.getStartedAt());
        }
        if (request.getStoppedAt() != null) {
            room.setStoppedAt(request.getStoppedAt());
        }
        if (request.getEstimatedEndTime() != null) {
            room.setEstimatedEndTime(request.getEstimatedEndTime());
        }
        // Nếu admin truyền status (ví dụ hoãn = 3) thì ưu tiên dùng, ngược lại dùng logic tự động
        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        } else {
            room.setStatus(determineStatus(room.getStartedAt(), room.getStoppedAt()));
        }
        room.setUpdatedAt(LocalDateTime.now());

        AuctionRoom saved = auctionRoomRepository.save(room);
        AdminAuctionRoomResponse response = mapToResponse(saved, true);
        UpdateResponse<AdminAuctionRoomResponse> success = new UpdateResponse<>(
                1, "Auction room updated successfully", response);
        return ResponseEntity.ok(success);
    }

    /**
     * Delete
     * Khi xóa phòng đấu giá, cần khôi phục status của các artwork về 1 (đã duyệt)
     * vì khi tạo phòng, status của artwork đã được chuyển thành 2 (đang đấu giá)
     */
    public ResponseEntity<AdminBasicResponse<Void>> deleteAuctionRoom(String roomId) {
        Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Auction room not found with ID: " + roomId, null));
        }

        AuctionRoom room = roomOpt.get();
        
        // Lấy tất cả sessions trong phòng đấu giá
        List<AuctionSession> sessions = auctionSessionRepository.findByAuctionRoomId(roomId);
        
        // Lấy danh sách artworkIds từ các sessions (distinct để tránh trùng lặp)
        List<String> artworkIds = sessions.stream()
                .map(AuctionSession::getArtworkId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        // Khôi phục status của các artwork về 1 (đã duyệt)
        if (!artworkIds.isEmpty()) {
            List<Artwork> artworks = artworkRepository.findAllById(artworkIds);
            for (Artwork artwork : artworks) {
                // Chỉ khôi phục nếu status hiện tại là 2 (đang đấu giá)
                // để tránh ảnh hưởng đến các artwork đã có status khác
                if (artwork.getStatus() == 2) {
                    artwork.setStatus(1); // Khôi phục về "Đã duyệt"
                    artwork.setUpdatedAt(LocalDateTime.now());
                    artworkRepository.save(artwork);
                }
            }
        }
        
        // Xóa toàn bộ sessions thuộc phòng đấu giá này
        // (nếu không xóa thì getAvailableArtworks sẽ luôn coi các artwork này là đã nằm trong session,
        //  dẫn đến không hiển thị lại dù status đã về 1)
        if (!sessions.isEmpty()) {
            auctionSessionRepository.deleteAll(sessions);
        }

        // Xóa phòng đấu giá
        auctionRoomRepository.delete(room);
        
        return ResponseEntity.ok(new AdminBasicResponse<>(1, "Auction room deleted successfully", null));
    }

    /**
     * Thống kê (bao gồm so sánh tháng này vs tháng trước)
     */
    public ResponseEntity<AuctionRoomStatisticsResponse> getAuctionRoomStatistics() {
        long total = auctionRoomRepository.count();
        long upcoming = auctionRoomRepository.countByStatus(0);
        long running = auctionRoomRepository.countByStatus(1);
        long completed = auctionRoomRepository.countByStatus(2);
        long cancelled = auctionRoomRepository.countByStatus(3); // hoãn

        // Lấy thống kê so sánh tháng này vs tháng trước
        MonthlyComparisonResponse monthlyComparison = monthlyStatisticsService.getMonthlyComparison("auction_rooms", "createdAt");
        MonthlyComparisonResponse.MonthlyComparisonData compData = monthlyComparison.getData();
        
        // Tạo monthly comparison data
        AuctionRoomStatisticsResponse.MonthlyComparison monthlyComp = new AuctionRoomStatisticsResponse.MonthlyComparison(
            compData.getCurrentMonth().getTotal(),
            compData.getPreviousMonth().getTotal(),
            compData.getChange().getAmount(),
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease(),
            compData.getCurrentMonth().getMonth(),
            compData.getPreviousMonth().getMonth()
        );

        AuctionRoomStatisticsResponse stats = new AuctionRoomStatisticsResponse(
                total, running, upcoming, completed, cancelled, monthlyComp
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy chi tiết phòng đấu giá theo ID
     */
    public ResponseEntity<?> getAuctionRoomDetail(String roomId) {
        Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Auction room not found with ID: " + roomId, null));
        }

        AuctionRoom room = roomOpt.get();
        boolean statusChanged = updateStatusIfNeeded(room);
        if (statusChanged) {
            auctionRoomRepository.save(room);
        }

        AuctionRoomDetailResponse detailResponse = buildAuctionRoomDetailResponse(room);
        return ResponseEntity.ok(detailResponse);
    }

    /**
     * Thống kê so sánh tháng này vs tháng trước cho auction rooms
     */
    public ResponseEntity<MonthlyComparisonResponse> getAuctionRoomMonthlyComparison() {
        MonthlyComparisonResponse response = monthlyStatisticsService.getMonthlyComparison("auction_rooms", "createdAt");
        return ResponseEntity.ok(response);
    }

    /**
     * Helpers
     */
    private int determineStatus(LocalDateTime startedAt, LocalDateTime stoppedAt) {
        LocalDateTime now = LocalDateTime.now();
        if (stoppedAt != null && !stoppedAt.isAfter(now)) {
            return 2; // Đã hoàn thành
        }
        if (startedAt != null && !startedAt.isAfter(now)) {
            return 1; // Đang diễn ra
        }
        return 0; // Sắp diễn ra
    }

    private AdminAuctionRoomResponse mapToResponseAndUpdateStatusIfNeeded(AuctionRoom room) {
        boolean changed = updateStatusIfNeeded(room);
        if (changed) {
            auctionRoomRepository.save(room);
        }
        return mapToResponse(room, false);
    }

    private boolean updateStatusIfNeeded(AuctionRoom room) {
        int currentStatus = room.getStatus();
        // Nếu phòng đang ở trạng thái hoãn (3) thì không tự động cập nhật lại
        if (currentStatus == 3) {
            return false;
        }
        int newStatus = determineStatus(room.getStartedAt(), room.getStoppedAt());
        if (currentStatus != newStatus) {
            room.setStatus(newStatus);
            room.setUpdatedAt(LocalDateTime.now());
            return true;
        }
        return false;
    }

    private AdminAuctionRoomResponse mapToResponse(AuctionRoom room, boolean skipStatusUpdate) {
        if (!skipStatusUpdate) {
            updateStatusIfNeeded(room);
        }
        AdminAuctionRoomResponse response = new AdminAuctionRoomResponse();
        response.setId(room.getId());
        response.setRoomName(room.getRoomName());
        response.setDescription(room.getDescription());
        response.setType(room.getType());
        response.setImageAuctionRoom(room.getImageAuctionRoom());
        response.setViewCount(room.getViewCount());
        response.setStatus(room.getStatus());
        response.setStartedAt(room.getStartedAt());
        response.setStoppedAt(room.getStoppedAt());
        response.setEstimatedEndTime(room.getEstimatedEndTime());
        response.setCreatedAt(room.getCreatedAt());

        PricePair pricePair = fetchPriceForRoom(room.getId());
        response.setStartingPrice(pricePair.startingPrice());
        response.setCurrentPrice(pricePair.currentPrice());

        // Tính tổng số người tham gia từ memberIds
        if (room.getMemberIds() != null) {
            response.setTotalMembers(room.getMemberIds().size());
        } else {
            response.setTotalMembers(0);
        }

        return response;
    }

    private PricePair fetchPriceForRoom(String roomId) {
        // Ưu tiên phiên mới nhất dựa trên startTime
        Optional<AuctionSession> sessionOpt =
                auctionSessionRepository.findFirstByAuctionRoomIdOrderByStartTimeDesc(roomId);

        if (sessionOpt.isEmpty()) {
            return new PricePair(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        AuctionSession session = sessionOpt.get();
        BigDecimal starting = session.getStartingPrice() == null ? BigDecimal.ZERO : session.getStartingPrice();
        BigDecimal current = session.getCurrentPrice() == null ? starting : session.getCurrentPrice();
        return new PricePair(starting, current);
    }

    /**
     * Tạo phòng đấu giá hoàn chỉnh với tất cả thông tin
     * Bao gồm: thông tin phòng, danh sách tác phẩm với giá, và cấu hình tài chính
     * imageAuctionRoom phải là URL string (đã được upload từ endpoint upload-ảnh)
     */
    public ResponseEntity<AdminBasicResponse<Map<String, Object>>> createAuctionRoomComplete(CreateAuctionRoomCompleteRequest request) {
        // Validate thông tin cơ bản
        if (!StringUtils.hasText(request.getRoomName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "roomName is required", null));
        }

        if (request.getStartedAt() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "startedAt is required", null));
        }

        if (!request.getStartedAt().isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "startedAt must be in the future", null));
        }

        if (request.getArtworks() == null || request.getArtworks().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "At least one artwork is required", null));
        }

        if (request.getDepositAmount() == null || request.getDepositAmount().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "depositAmount is required and must be >= 0", null));
        }

        if (request.getPaymentDeadlineDays() == null || request.getPaymentDeadlineDays() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "paymentDeadlineDays is required and must be > 0", null));
        }

        // Validate và lấy thông tin artworks
        List<Artwork> artworks = new ArrayList<>();
        for (ArtworkPriceSetting priceSetting : request.getArtworks()) {
            Optional<Artwork> artworkOpt = artworkRepository.findById(priceSetting.getArtworkId());
            if (artworkOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AdminBasicResponse<>(0, "Artwork not found: " + priceSetting.getArtworkId(), null));
            }

            Artwork artwork = artworkOpt.get();
            // Validate startingPrice và bidStep
            if (priceSetting.getStartingPrice() == null || 
                priceSetting.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AdminBasicResponse<>(0, "startingPrice must be > 0 for artwork: " + artwork.getTitle(), null));
            }

            if (priceSetting.getBidStep() == null || 
                priceSetting.getBidStep().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AdminBasicResponse<>(0, "bidStep must be > 0 for artwork: " + artwork.getTitle(), null));
            }

            artworks.add(artwork);
        }

        // Tạo AuctionRoom
        AuctionRoom room = new AuctionRoom();
        room.generateId();
        room.setRoomName(request.getRoomName());
        room.setDescription(request.getDescription());
        room.setType(request.getType());
        room.setImageAuctionRoom(request.getImageAuctionRoom()); // URL từ endpoint upload-ảnh
        room.setStartedAt(request.getStartedAt());
        room.setStoppedAt(request.getStoppedAt());
        room.setEstimatedEndTime(request.getEstimatedEndTime());
        room.setViewCount(0);
        room.setStatus(determineStatus(request.getStartedAt(), request.getStoppedAt()));
        room.setMemberIds(new ArrayList<>());
        room.setAdminId(request.getAdminId());
        room.setDepositAmount(request.getDepositAmount());
        room.setPaymentDeadlineDays(request.getPaymentDeadlineDays());
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        AuctionRoom savedRoom = auctionRoomRepository.save(room);

        // Tạo AuctionSession cho từng artwork
        List<AuctionSession> sessions = new ArrayList<>();
        int orderIndex = 1;
        LocalDateTime currentStartTime = request.getStartedAt();

        for (int i = 0; i < request.getArtworks().size(); i++) {
            ArtworkPriceSetting priceSetting = request.getArtworks().get(i);
            Artwork artwork = artworks.get(i);

            AuctionSession session = new AuctionSession();
            session.generateId();
            session.setAuctionRoomId(savedRoom.getId());
            session.setArtworkId(artwork.getId());
            session.setImageUrl(artwork.getAvtArtwork());
            session.setStartingPrice(priceSetting.getStartingPrice());
            session.setCurrentPrice(priceSetting.getStartingPrice()); // Ban đầu = startingPrice
            session.setBidStep(priceSetting.getBidStep());
            session.setStatus(0); // DRAFT - chưa bắt đầu
            session.setViewCount(0);
            session.setBidCount(0);
            session.setOrderIndex(orderIndex++);
            session.setSellerId(artwork.getOwnerId());
            session.setType(request.getType());
            session.setEndedAt(null);

            // Tính toán thời gian cho từng session
            // Giả sử mỗi session kéo dài 10 phút (có thể điều chỉnh)
            session.setStartTime(null);
//            session.setEndedAt(currentStartTime.plusMinutes(10)); // Có thể điều chỉnh logic này

            // Cập nhật thời gian bắt đầu cho session tiếp theo
            currentStartTime = currentStartTime.plusMinutes(10);

            sessions.add(session);
        }

        // Lưu tất cả sessions
        auctionSessionRepository.saveAll(sessions);

        // Cập nhật stoppedAt của room nếu chưa có (dựa trên session cuối cùng)
        if (savedRoom.getStoppedAt() == null && !sessions.isEmpty()) {
            AuctionSession lastSession = sessions.get(sessions.size() - 1);
            savedRoom.setStoppedAt(lastSession.getEndedAt());
            auctionRoomRepository.save(savedRoom);
        }

        // Cập nhật status của artworks thành "Đang đấu giá" (2)
        for (Artwork artwork : artworks) {
            artwork.setStatus(2); // Đang đấu giá
            artworkRepository.save(artwork);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("roomId", savedRoom.getId());
        data.put("sessionsCreated", sessions.size());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminBasicResponse<>(1, "Auction room created successfully", data));
    }

    /**
     * Upload ảnh phòng đấu giá từ thiết bị và trả về URL
     * URL này sẽ được dùng trong field imageAuctionRoom của endpoint tạo phòng
     */
    public ResponseEntity<AdminBasicResponse<Map<String, String>>> uploadAuctionRoomImage(MultipartFile imageAuctionRoomFile) {
        if (imageAuctionRoomFile == null || imageAuctionRoomFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "File ảnh không được để trống", null));
        }

        try {
            // Tạo ID tạm thời cho room (sẽ được dùng để tạo folder trên Cloudinary)
            // Hoặc có thể dùng một ID tạm thời
            String tempRoomId = "temp-" + System.currentTimeMillis();
            CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadAuctionRoomImage(tempRoomId, imageAuctionRoomFile);
            
            Map<String, String> data = new HashMap<>();
            data.put("imageUrl", uploadResult.getUrl());
            data.put("publicId", uploadResult.getPublicId());
            
            return ResponseEntity.ok(new AdminBasicResponse<>(1, "Upload ảnh thành công", data));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminBasicResponse<>(0, "Failed to upload image: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách tất cả artworks có thể thêm vào phòng đấu giá
     * Chỉ lấy artworks đã được duyệt (status = 1) và:
     * - Chưa được thêm vào session (bất kỳ session nào)
     * - Chưa được tạo hóa đơn trong invoice
     */
    public ResponseEntity<List<ArtworkForSelectionResponse>> getAvailableArtworks() {
        // Lấy tất cả artworks đã được duyệt (status = 1)
        List<Artwork> approvedArtworks = artworkRepository.findByStatus(1);
        System.out.println("DEBUG getAvailableArtworks - Total approved artworks: " + approvedArtworks.size());
        
        // Lấy danh sách room hiện còn tồn tại
        List<AuctionRoom> allRooms = auctionRoomRepository.findAll();
        Set<String> existingRoomIds = allRooms.stream()
                .map(AuctionRoom::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Lấy danh sách artworkIds đã có trong session,
        // nhưng CHỈ tính các session thuộc những phòng vẫn còn tồn tại
        List<AuctionSession> allSessions = auctionSessionRepository.findAll();
        Set<String> artworkIdsInSessions = allSessions.stream()
                .filter(session -> session.getAuctionRoomId() != null
                        && existingRoomIds.contains(session.getAuctionRoomId()))
            .map(AuctionSession::getArtworkId)
            .filter(id -> id != null && !id.isEmpty())
            .collect(Collectors.toSet());
        
        System.out.println("DEBUG getAvailableArtworks - Total sessions: " + allSessions.size());
        System.out.println("DEBUG getAvailableArtworks - Existing rooms: " + existingRoomIds.size());
        System.out.println("DEBUG getAvailableArtworks - Artwork IDs in sessions (existing rooms only): " + artworkIdsInSessions.size());
        
        // Lấy danh sách artworkIds đã có trong invoice (đã được tạo hóa đơn)
        List<Invoice> allInvoices = invoiceRepository.findAll();
        Set<String> artworkIdsInInvoices = allInvoices.stream()
            .map(Invoice::getArtworkId)
            .filter(id -> id != null && !id.isEmpty())
            .collect(Collectors.toSet());
        
        System.out.println("DEBUG getAvailableArtworks - Total invoices: " + allInvoices.size());
        System.out.println("DEBUG getAvailableArtworks - Artwork IDs in invoices: " + artworkIdsInInvoices.size());
        
        // Kết hợp 2 set để loại bỏ
        Set<String> excludedArtworkIds = new HashSet<>();
        excludedArtworkIds.addAll(artworkIdsInSessions);
        excludedArtworkIds.addAll(artworkIdsInInvoices);
        
        System.out.println("DEBUG getAvailableArtworks - Total excluded artwork IDs: " + excludedArtworkIds.size());
        
        // Filter: chỉ lấy artworks chưa có trong session và chưa có trong invoice
        List<Artwork> availableArtworks = approvedArtworks.stream()
            .filter(artwork -> !excludedArtworkIds.contains(artwork.getId()))
            .collect(Collectors.toList());
        
        System.out.println("DEBUG getAvailableArtworks - Available artworks after filter: " + availableArtworks.size());
        
        // Load thông tin user (author) cho tất cả artworks
        List<String> ownerIds = availableArtworks.stream()
            .map(Artwork::getOwnerId)
            .filter(id -> id != null && !id.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        
        Map<String, User> userMap = userRepository.findAllById(ownerIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        
        // Map sang ArtworkForSelectionResponse
        List<ArtworkForSelectionResponse> responses = availableArtworks.stream()
            .map(artwork -> {
                ArtworkForSelectionResponse response = new ArtworkForSelectionResponse();
                response.setId(artwork.getId());
                response.setTitle(artwork.getTitle());
                response.setDescription(artwork.getDescription());
                response.setPaintingGenre(artwork.getPaintingGenre());
                response.setMaterial(artwork.getMaterial());
                response.setSize(artwork.getSize());
                response.setAvtArtwork(artwork.getAvtArtwork());
                response.setStartedPrice(artwork.getStartedPrice());
                response.setStatus(artwork.getStatus());
                
                // Lấy author name từ ownerId
                User owner = userMap.get(artwork.getOwnerId());
                if (owner != null) {
                    response.setAuthor(owner.getUsername());
                } else {
                    response.setAuthor("Unknown");
                }
                
                return response;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    private record PricePair(BigDecimal startingPrice, BigDecimal currentPrice) {}

    private AuctionRoomDetailResponse buildAuctionRoomDetailResponse(AuctionRoom room) {
        AuctionRoomDetailResponse detail = new AuctionRoomDetailResponse();
        detail.setId(room.getId());
        detail.setRoomName(room.getRoomName());
        detail.setDescription(room.getDescription());
        detail.setType(room.getType());
        detail.setImageAuctionRoom(room.getImageAuctionRoom());
        detail.setStartedAt(room.getStartedAt());
        detail.setStoppedAt(room.getStoppedAt());
        detail.setEstimatedEndTime(room.getEstimatedEndTime());
        detail.setStatus(room.getStatus());
        detail.setDepositAmount(room.getDepositAmount() != null ? room.getDepositAmount() : BigDecimal.ZERO);
        detail.setViewCount(room.getViewCount() != null ? room.getViewCount() : 0);
        detail.setTotalMembers(room.getMemberIds() == null ? 0 : room.getMemberIds().size());
        detail.setCreatedAt(room.getCreatedAt());
        detail.setUpdatedAt(room.getUpdatedAt());

        if (StringUtils.hasText(room.getAdminId())) {
            AuctionRoomDetailResponse.AdminInfo adminInfo = adminRepository.findById(room.getAdminId())
                    .map(admin -> new AuctionRoomDetailResponse.AdminInfo(
                            admin.getId(),
                            admin.getFullName(),
                            admin.getEmail(),
                            admin.getPhoneNumber()
                    ))
                    .orElseGet(() -> new AuctionRoomDetailResponse.AdminInfo(
                            room.getAdminId(),
                            "Unknown admin",
                            null,
                            null
                    ));
            detail.setAdmin(adminInfo);
        } else {
            detail.setAdmin(null);
        }

        // Lấy TẤT CẢ sessions trong phòng đấu giá (không filter theo status hay điều kiện nào)
        // Repository method findByAuctionRoomId() trả về TẤT CẢ sessions, không phân biệt status
        List<AuctionSession> sessions = auctionSessionRepository.findByAuctionRoomId(room.getId());
        if (sessions.isEmpty()) {
            detail.setArtworks(Collections.emptyList());
            return detail;
        }

        // Debug: Log số lượng sessions để đảm bảo lấy đủ
        System.out.println("DEBUG buildAuctionRoomDetailResponse - Total sessions in room " + room.getId() + ": " + sessions.size());

        // Sắp xếp sessions theo thời gian và order
        sessions.sort(Comparator
                .comparing(AuctionSession::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(AuctionSession::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(AuctionSession::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)));

        // Lấy danh sách artworkIds để load artwork data (distinct chỉ để tối ưu query, không ảnh hưởng số lượng sessions trả về)
        List<String> artworkIds = sessions.stream()
                .map(AuctionSession::getArtworkId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Artwork> artworkMap = artworkIds.isEmpty()
                ? Collections.emptyMap()
                : artworkRepository.findAllById(artworkIds).stream()
                .collect(Collectors.toMap(Artwork::getId, a -> a));

        List<String> ownerIds = artworkMap.values().stream()
                .map(Artwork::getOwnerId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, User> ownerMap = ownerIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Map TẤT CẢ sessions thành response (KHÔNG filter theo status, KHÔNG filter theo artwork status)
        // Trả về TẤT CẢ sessions có trong phòng, bất kể status của session hay artwork
        List<AuctionRoomDetailResponse.SessionArtworkInfo> artworkInfos = sessions.stream()
                .map(session -> {
                    Artwork artwork = artworkMap.get(session.getArtworkId());
                    User owner = artwork != null ? ownerMap.get(artwork.getOwnerId()) : null;

                    AuctionRoomDetailResponse.SessionArtworkInfo info = new AuctionRoomDetailResponse.SessionArtworkInfo();
                    info.setSessionId(session.getId());
                    info.setArtworkId(session.getArtworkId());
                    info.setArtworkName(artwork != null ? artwork.getTitle() : "Unknown artwork");
                    info.setAuthor(owner != null ? owner.getUsername() : "Unknown author");
                    info.setAvtArtwork(artwork != null ? artwork.getAvtArtwork() : null);
                    info.setStartingPrice(safeAmount(session.getStartingPrice()));
                    info.setCurrentPrice(safeCurrentPrice(session));
                    info.setBidStep(safeAmount(session.getBidStep()));
                    info.setStatus(session.getStatus());

                    // Thông tin chi tiết từ Artwork
                    if (artwork != null) {
                        info.setImageUrls(artwork.getImageUrls());
                        info.setAiVerified(artwork.isAiVerified());
                        info.setSize(artwork.getSize());
                        info.setMaterial(artwork.getMaterial());
                        info.setCertificateId(artwork.getCertificateId());
                        info.setPaintingGenre(artwork.getPaintingGenre());
                        info.setYearOfCreation(artwork.getYearOfCreation());
                        info.setDescription(artwork.getDescription());
                    }

                    return info;
                })
                .collect(Collectors.toList());

        detail.setArtworks(artworkInfos);
        return detail;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal safeCurrentPrice(AuctionSession session) {
        if (session.getCurrentPrice() != null) {
            return session.getCurrentPrice();
        }
        if (session.getStartingPrice() != null) {
            return session.getStartingPrice();
        }
        return BigDecimal.ZERO;
    }

}

