package AdminBackend.Service;

import AdminBackend.DTO.Request.AddAuctionRoomRequest;
import AdminBackend.DTO.Request.ArtworkPriceSetting;
import AdminBackend.DTO.Request.CreateAuctionRoomCompleteRequest;
import AdminBackend.DTO.Request.UpdateAuctionRoomRequest;
import AdminBackend.DTO.Response.AdminAuctionRoomResponse;
import AdminBackend.DTO.Response.AdminBasicResponse;
import AdminBackend.DTO.Response.AuctionRoomStatisticsResponse;
import AdminBackend.DTO.Response.MonthlyComparisonResponse;
import AdminBackend.DTO.Response.UpdateResponse;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    private MonthlyStatisticsService monthlyStatisticsService;

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
     * Lấy tất cả phòng đấu giá
     */
    public ResponseEntity<List<AdminAuctionRoomResponse>> getAllAuctionRooms() {
        List<AuctionRoom> rooms = auctionRoomRepository.findAll();
        List<AdminAuctionRoomResponse> responses = rooms.stream()
                .map(this::mapToResponseAndUpdateStatusIfNeeded)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Tìm kiếm theo room id hoặc roomName
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

        List<AdminAuctionRoomResponse> responses = resultSet.stream()
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

        room.setStatus(determineStatus(room.getStartedAt(), room.getStoppedAt()));
        room.setUpdatedAt(LocalDateTime.now());

        AuctionRoom saved = auctionRoomRepository.save(room);
        AdminAuctionRoomResponse response = mapToResponse(saved, true);
        UpdateResponse<AdminAuctionRoomResponse> success = new UpdateResponse<>(
                1, "Auction room updated successfully", response);
        return ResponseEntity.ok(success);
    }

    /**
     * Delete
     */
    public ResponseEntity<AdminBasicResponse<Void>> deleteAuctionRoom(String roomId) {
        Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Auction room not found with ID: " + roomId, null));
        }

        auctionRoomRepository.delete(roomOpt.get());
        return ResponseEntity.ok(new AdminBasicResponse<>(1, "Auction room deleted successfully", null));
    }

    /**
     * Thống kê
     */
    public ResponseEntity<AuctionRoomStatisticsResponse> getAuctionRoomStatistics() {
        long total = auctionRoomRepository.count();
        long upcoming = auctionRoomRepository.countByStatus(0);
        long running = auctionRoomRepository.countByStatus(1);
        long completed = auctionRoomRepository.countByStatus(2);

        AuctionRoomStatisticsResponse stats = new AuctionRoomStatisticsResponse(
                total, running, upcoming, completed
        );

        return ResponseEntity.ok(stats);
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
        response.setCreatedAt(room.getCreatedAt());

        PricePair pricePair = fetchPriceForRoom(room.getId());
        response.setStartingPrice(pricePair.startingPrice());
        response.setCurrentPrice(pricePair.currentPrice());

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
        room.setImageAuctionRoom(request.getImageAuctionRoom());
        room.setStartedAt(request.getStartedAt());
        room.setStoppedAt(request.getStoppedAt());
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

            // Tính toán thời gian cho từng session
            // Giả sử mỗi session kéo dài 10 phút (có thể điều chỉnh)
            session.setStartTime(currentStartTime);
            session.setEndedAt(currentStartTime.plusMinutes(10)); // Có thể điều chỉnh logic này

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

    private record PricePair(BigDecimal startingPrice, BigDecimal currentPrice) {}
}

