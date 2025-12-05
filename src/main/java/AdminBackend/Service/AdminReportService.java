package AdminBackend.Service;

import AdminBackend.DTO.Request.ProcessReportRequest;
import AdminBackend.DTO.Request.UpdateReportRequest;
import AdminBackend.DTO.Response.AdminReportApiResponse;
import AdminBackend.DTO.Response.AdminReportResponse;
import AdminBackend.DTO.Response.AdminReportStatisticsResponse;
import AdminBackend.DTO.Response.MonthlyComparisonResponse;
import com.auctionaa.backend.Constants.ReportConstants;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.Notification;
import com.auctionaa.backend.Entity.ReportObject;
import com.auctionaa.backend.Entity.Reports;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.NotificationRepository;
import com.auctionaa.backend.Repository.ReportObjectRepository;
import com.auctionaa.backend.Repository.ReportsRepository;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.EmailService;
import com.auctionaa.backend.Service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminReportService {

    @Autowired
    private ReportsRepository reportsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportObjectRepository reportObjectRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MonthlyStatisticsService monthlyStatisticsService;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private AuctionRoomRepository auctionRoomRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    public ResponseEntity<AdminReportApiResponse<List<AdminReportResponse>>> getAllReports() {
        // Đọc trực tiếp từ MongoDB để lấy DBRef
        List<Document> reportDocuments = mongoTemplate.getCollection("reports")
                .find()
                .sort(new Document("createdAt", -1))
                .into(new java.util.ArrayList<>());

        List<AdminReportResponse> data = reportDocuments.stream()
                .map(this::mapDocumentToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AdminReportApiResponse<>(1, "Lấy danh sách báo cáo thành công", data));
    }

    public ResponseEntity<AdminReportApiResponse<List<AdminReportResponse>>> searchReports(String searchTerm) {
        List<Document> reportDocuments;
        if (!StringUtils.hasText(searchTerm)) {
            reportDocuments = mongoTemplate.getCollection("reports")
                    .find()
                    .sort(new Document("createdAt", -1))
                    .into(new java.util.ArrayList<>());
        } else {
            // Tìm kiếm với regex
            Document query = new Document("$or", java.util.Arrays.asList(
                    new Document("_id", new Document("$regex", searchTerm).append("$options", "i")),
                    new Document("reportReason", new Document("$regex", searchTerm).append("$options", "i"))
            ));
            reportDocuments = mongoTemplate.getCollection("reports")
                    .find(query)
                    .sort(new Document("createdAt", -1))
                    .into(new java.util.ArrayList<>());
        }

        List<AdminReportResponse> data = reportDocuments.stream()
                .map(this::mapDocumentToResponse)
                .collect(Collectors.toList());
        String message = StringUtils.hasText(searchTerm)
                ? String.format("Tìm thấy %d báo cáo cho từ khóa '%s'", data.size(), searchTerm)
                : "Lấy danh sách báo cáo thành công";
        return ResponseEntity.ok(new AdminReportApiResponse<>(1, message, data));
    }

    public ResponseEntity<AdminReportApiResponse<AdminReportResponse>> updateReport(
            String reportId,
            UpdateReportRequest request) {
        Optional<Reports> optionalReports = reportsRepository.findById(reportId);
        if (optionalReports.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminReportApiResponse<>(0, "Không tìm thấy báo cáo", null));
        }

        Reports report = optionalReports.get();

        if (StringUtils.hasText(request.getReportReason())) {
            report.setReportReason(request.getReportReason());
        }
        if (request.getReportStatus() != null) {
            report.setReportStatus(request.getReportStatus());
            if (request.getReportStatus() == 2 && report.getResolvedAt() == null) {
                report.setResolvedAt(LocalDateTime.now());
            }
        }
        if (request.getReportDoneTime() != null) {
            report.setResolvedAt(request.getReportDoneTime());
        }

        Reports updated = reportsRepository.save(report);
        // Đọc lại từ MongoDB để lấy DBRef đầy đủ
        Document updatedDoc = mongoTemplate.getCollection("reports")
                .find(new Document("_id", reportId))
                .first();
        AdminReportResponse response = updatedDoc != null 
                ? mapDocumentToResponse(updatedDoc)
                : mapToResponse(updated);
        return ResponseEntity.ok(new AdminReportApiResponse<>(1, "Cập nhật báo cáo thành công", response));
    }

    public ResponseEntity<AdminReportApiResponse<Void>> deleteReport(String reportId) {
        Optional<Reports> optionalReports = reportsRepository.findById(reportId);
        if (optionalReports.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminReportApiResponse<>(0, "Không tìm thấy báo cáo", null));
        }

        reportsRepository.delete(optionalReports.get());
        return ResponseEntity.ok(new AdminReportApiResponse<>(1, "Xóa báo cáo thành công", null));
    }

    /**
     * Xử lý báo cáo theo action của admin
     * 
     * Nghiệp vụ:
     * - User Reports (entityType = 1): 
     *   - "WARNING": Gửi notification và email cảnh báo cho user
     *   - "BLOCK": Chặn user (đổi status = 2) và gửi email thông báo
     *   - "DISMISS": Từ chối báo cáo (status = 3), không có hành động gì
     * 
     * - Artwork Reports (entityType = 2):
     *   - "REJECT": Từ chối artwork (status = 3) và gửi email cho owner
     *   - "DISMISS": Từ chối báo cáo (status = 3), không có hành động gì
     * 
     * - Auction Room Reports (entityType = 3):
     *   - "CLOSE": Đóng room (status = 0) và gửi email cho admin/host
     *   - "DISMISS": Từ chối báo cáo (status = 3), không có hành động gì
     * 
     * - AI Artwork Reports (entityType = 4):
     *   - "REJECT": Từ chối artwork (status = 3) và gửi email cho owner
     *   - "DISMISS": Từ chối báo cáo (status = 3), không có hành động gì
     */
    public ResponseEntity<AdminReportApiResponse<AdminReportResponse>> processReport(
            String reportId, ProcessReportRequest request) {
        
        Optional<Reports> optionalReport = reportsRepository.findById(reportId);
        if (optionalReport.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminReportApiResponse<>(0, "Không tìm thấy báo cáo", null));
        }

        Reports report = optionalReport.get();
        
        // Validate action
        if (request == null || !StringUtils.hasText(request.getAction())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminReportApiResponse<>(0, "Action là bắt buộc (WARNING, BLOCK, REJECT, CLOSE, DISMISS)", null));
        }

        String action = request.getAction().toUpperCase().trim();
        String adminNote = request.getAdminNote();

        // Lấy entityType và reportedEntityId
        // Sử dụng cấu trúc mới nếu có, fallback về cấu trúc cũ
        int entityType = report.getEntityType();
        String reportedEntityId = report.getReportedEntityId();
        
        // Fallback về cấu trúc cũ nếu không có entityType hoặc reportedEntityId
        if (entityType == 0 || !StringUtils.hasText(reportedEntityId)) {
            if (StringUtils.hasText(report.getObjectId())) {
                // Xác định entityType từ objectId (cần check xem là user, artwork hay room)
                String objectId = report.getObjectId();
                if (userRepository.existsById(objectId)) {
                    entityType = ReportConstants.ENTITY_USER;
                    reportedEntityId = objectId;
                } else if (artworkRepository.existsById(objectId)) {
                    entityType = ReportConstants.ENTITY_ARTWORK;
                    reportedEntityId = objectId;
                } else if (auctionRoomRepository.existsById(objectId)) {
                    entityType = ReportConstants.ENTITY_AUCTION_ROOM;
                    reportedEntityId = objectId;
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new AdminReportApiResponse<>(0, "Không thể xác định loại entity từ objectId", null));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AdminReportApiResponse<>(0, "Không tìm thấy thông tin entity bị báo cáo", null));
            }
        }

        // Validate action với entityType
        if (!isValidActionForEntityType(action, entityType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminReportApiResponse<>(0, 
                        String.format("Action '%s' không hợp lệ cho entityType %d (%s)", 
                            action, entityType, ReportConstants.getEntityTypeName(entityType)), null));
        }

        try {
            // Xử lý theo entityType và action
            switch (entityType) {
                case ReportConstants.ENTITY_USER:
                    processUserReport(report, reportedEntityId, action, adminNote);
                    break;
                case ReportConstants.ENTITY_ARTWORK:
                case ReportConstants.ENTITY_AI_ARTWORK:
                    processArtworkReport(report, reportedEntityId, action, adminNote);
                    break;
                case ReportConstants.ENTITY_AUCTION_ROOM:
                    processAuctionRoomReport(report, reportedEntityId, action, adminNote);
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new AdminReportApiResponse<>(0, "EntityType không hợp lệ", null));
            }

            // Cập nhật report status
            if ("DISMISS".equals(action)) {
                report.setStatus(ReportConstants.STATUS_REJECTED);
            } else {
                report.setStatus(ReportConstants.STATUS_RESOLVED);
            }
            report.setAdminNote(adminNote);
            report.setResolvedAt(LocalDateTime.now());
            report.setUpdatedAt(LocalDateTime.now());
            reportsRepository.save(report);

            // Đọc lại từ MongoDB để lấy DBRef đầy đủ
            Document updatedDoc = mongoTemplate.getCollection("reports")
                    .find(new Document("_id", reportId))
                    .first();
            AdminReportResponse response = updatedDoc != null 
                    ? mapDocumentToResponse(updatedDoc)
                    : mapToResponse(report);

            return ResponseEntity.ok(new AdminReportApiResponse<>(1, "Xử lý báo cáo thành công", response));

        } catch (Exception e) {
            log.error("Error processing report {}: {}", reportId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminReportApiResponse<>(0, "Lỗi khi xử lý báo cáo: " + e.getMessage(), null));
        }
    }

    /**
     * Validate action có hợp lệ với entityType không
     */
    private boolean isValidActionForEntityType(String action, int entityType) {
        return switch (entityType) {
            case ReportConstants.ENTITY_USER -> 
                "WARNING".equals(action) || "BLOCK".equals(action) || "DISMISS".equals(action);
            case ReportConstants.ENTITY_ARTWORK, ReportConstants.ENTITY_AI_ARTWORK -> 
                "REJECT".equals(action) || "DISMISS".equals(action);
            case ReportConstants.ENTITY_AUCTION_ROOM -> 
                "CLOSE".equals(action) || "DISMISS".equals(action);
            default -> false;
        };
    }

    /**
     * Xử lý User Report
     * Nếu user đã bị báo cáo 3 lần (đã xử lý với WARNING hoặc BLOCK), 
     * thì không cho WARNING nữa, bắt buộc phải BLOCK
     */
    private void processUserReport(Reports report, String userId, String action, String adminNote) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID không được để trống");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy user với ID: " + userId);
        }

        User user = userOpt.get();
        String reportType = report.getReportType() != null ? report.getReportType() : "Báo cáo";

        // Đếm số lần user đã bị báo cáo và đã được xử lý (status = 2 Resolved hoặc 3 Rejected, nhưng không tính DISMISS)
        long processedReportCount = countProcessedUserReports(userId);
        
        // Nếu đã 3 lần và action là WARNING, thì bắt buộc phải BLOCK
        if ("WARNING".equals(action) && processedReportCount >= 3) {
            throw new IllegalArgumentException(
                String.format("User đã bị báo cáo %d lần. Không thể cảnh báo thêm, bắt buộc phải chặn tài khoản (action = BLOCK)", 
                    processedReportCount));
        }

        // Lấy reason từ report (ưu tiên reason, nếu không có thì dùng reportReason)
        String reportReason = StringUtils.hasText(report.getReason()) 
            ? report.getReason() 
            : (StringUtils.hasText(report.getReportReason()) ? report.getReportReason() : null);

        switch (action) {
            case "WARNING":
                // Gửi notification và email cảnh báo
                sendUserWarningNotification(user, reportType, reportReason, adminNote, processedReportCount + 1);
                sendUserWarningEmail(user, reportType, reportReason, adminNote, processedReportCount + 1);
                break;
            case "BLOCK":
                // Chặn user (status = 2)
                user.setStatus(2);
                userRepository.save(user);
                // Gửi email thông báo bị chặn
                sendUserBlockedEmail(user, reportType, reportReason, adminNote);
                // Gửi notification
                sendUserBlockedNotification(user, reportType, reportReason, adminNote);
                break;
            case "DISMISS":
                // Không có hành động gì, chỉ từ chối báo cáo
                break;
        }
    }

    /**
     * Đếm số lần user đã bị báo cáo và đã được xử lý (không tính DISMISS)
     * Chỉ đếm các report có status = 2 (Resolved) hoặc 3 (Rejected) 
     * nhưng không phải DISMISS (tức là đã có hành động thực sự)
     */
    private long countProcessedUserReports(String userId) {
        // Lấy tất cả reports về user này (entityType = 1)
        List<Reports> userReports = reportsRepository.findByEntityTypeAndReportedEntityId(
            ReportConstants.ENTITY_USER, userId);
        
        // Đếm các report đã được xử lý (status = 2 Resolved hoặc 3 Rejected)
        // Nhưng không tính các report có adminNote rỗng hoặc null (có thể là DISMISS)
        // Hoặc đơn giản hơn: đếm tất cả report có status = 2 (Resolved) 
        // vì DISMISS sẽ có status = 3 (Rejected)
        return userReports.stream()
            .filter(r -> r.getStatus() == ReportConstants.STATUS_RESOLVED) // Chỉ đếm Resolved (đã xử lý với WARNING hoặc BLOCK)
            .count();
    }

    /**
     * Xử lý Artwork Report (bao gồm cả AI Artwork)
     */
    private void processArtworkReport(Reports report, String artworkId, String action, String adminNote) {
        if (!StringUtils.hasText(artworkId)) {
            throw new IllegalArgumentException("Artwork ID không được để trống");
        }

        Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
        if (artworkOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy artwork với ID: " + artworkId);
        }

        Artwork artwork = artworkOpt.get();
        String reportType = report.getReportType() != null ? report.getReportType() : "Báo cáo";

        switch (action) {
            case "REJECT":
                // Từ chối artwork (status = 3)
                artwork.setStatus(3);
                artwork.setUpdatedAt(LocalDateTime.now());
                artworkRepository.save(artwork);
                
                // Lấy reason từ report
                String reportReason = StringUtils.hasText(report.getReason()) 
                    ? report.getReason() 
                    : (StringUtils.hasText(report.getReportReason()) ? report.getReportReason() : null);
                
                // Gửi email cho owner
                if (StringUtils.hasText(artwork.getOwnerId())) {
                    Optional<User> ownerOpt = userRepository.findById(artwork.getOwnerId());
                    if (ownerOpt.isPresent()) {
                        User owner = ownerOpt.get();
                        sendArtworkRejectedByReportEmail(owner, artwork, reportType, reportReason, adminNote);
                        sendArtworkRejectedNotification(owner, artwork, reportType, reportReason);
                    }
                }
                break;
            case "DISMISS":
                // Không có hành động gì, chỉ từ chối báo cáo
                break;
        }
    }

    /**
     * Xử lý Auction Room Report
     */
    private void processAuctionRoomReport(Reports report, String roomId, String action, String adminNote) {
        if (!StringUtils.hasText(roomId)) {
            throw new IllegalArgumentException("Auction Room ID không được để trống");
        }

        Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy auction room với ID: " + roomId);
        }

        AuctionRoom room = roomOpt.get();
        String reportType = report.getReportType() != null ? report.getReportType() : "Báo cáo";

        switch (action) {
            case "CLOSE":
                // Đóng room (status = 0)
                room.setStatus(0);
                auctionRoomRepository.save(room);
                
                // Lấy reason từ report
                String reportReason = StringUtils.hasText(report.getReason()) 
                    ? report.getReason() 
                    : (StringUtils.hasText(report.getReportReason()) ? report.getReportReason() : null);
                
                // Gửi email cho admin/host
                if (StringUtils.hasText(room.getAdminId())) {
                    Optional<User> adminOpt = userRepository.findById(room.getAdminId());
                    if (adminOpt.isPresent()) {
                        User admin = adminOpt.get();
                        sendRoomClosedEmail(admin, room, reportType, reportReason, adminNote);
                        sendRoomClosedNotification(admin, room, reportType, reportReason);
                    }
                }
                break;
            case "DISMISS":
                // Không có hành động gì, chỉ từ chối báo cáo
                break;
        }
    }

    // ========== Notification Methods ==========

    private void sendUserWarningNotification(User user, String reportType, String reason, String adminNote, long reportCount) {
        try {
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setNotificationType(1); // Warning type
            notification.setTitle("Cảnh báo từ hệ thống");
            String warningMessage = String.format("Bạn đã nhận được cảnh báo về: %s. ", reportType);
            if (StringUtils.hasText(reason)) {
                warningMessage += String.format("Lý do: %s. ", reason);
            }
            if (reportCount >= 2) {
                warningMessage += String.format("Đây là lần cảnh báo thứ %d. ", reportCount);
                if (reportCount == 2) {
                    warningMessage += "Lần vi phạm tiếp theo sẽ dẫn đến việc tài khoản bị chặn. ";
                }
            }
            warningMessage += adminNote != null ? adminNote : "Vui lòng tuân thủ quy định của nền tảng.";
            notification.setNotificationContent(warningMessage);
            notification.setNotificationStatus(1);
            notification.setLink("/profile");
            
            log.info("Creating warning notification for user: {}", user.getId());
            Notification saved = notificationService.addNotification(notification);
            log.info("Warning notification created successfully with ID: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to send warning notification to user {}: {}", user.getId(), e.getMessage(), e);
            // Re-throw để admin biết có lỗi xảy ra
            throw new RuntimeException("Không thể tạo thông báo cảnh báo: " + e.getMessage(), e);
        }
    }

    private void sendUserBlockedNotification(User user, String reportType, String reason, String adminNote) {
        try {
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setNotificationType(2); // Block type
            notification.setTitle("Tài khoản của bạn đã bị chặn");
            String blockedMessage = String.format("Tài khoản của bạn đã bị chặn do: %s. ", reportType);
            if (StringUtils.hasText(reason)) {
                blockedMessage += String.format("Lý do: %s. ", reason);
            }
            blockedMessage += adminNote != null ? adminNote : "Vui lòng liên hệ admin để được hỗ trợ.";
            notification.setNotificationContent(blockedMessage);
            notification.setNotificationStatus(1);
            notification.setLink("/support");
            
            log.info("Creating blocked notification for user: {}", user.getId());
            Notification saved = notificationService.addNotification(notification);
            log.info("Blocked notification created successfully with ID: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to send blocked notification to user {}: {}", user.getId(), e.getMessage(), e);
            // Re-throw để admin biết có lỗi xảy ra
            throw new RuntimeException("Không thể tạo thông báo bị chặn: " + e.getMessage(), e);
        }
    }

    private void sendArtworkRejectedNotification(User owner, Artwork artwork, String reportType, String reason) {
        try {
            Notification notification = new Notification();
            notification.setUserId(owner.getId());
            notification.setNotificationType(3); // Artwork rejected type
            notification.setTitle("Tác phẩm của bạn đã bị từ chối");
            String rejectedMessage = String.format("Tác phẩm '%s' đã bị từ chối do: %s", artwork.getTitle(), reportType);
            if (StringUtils.hasText(reason)) {
                rejectedMessage += String.format(". Lý do: %s", reason);
            }
            notification.setNotificationContent(rejectedMessage);
            notification.setNotificationStatus(1);
            notification.setLink("/artworks/" + artwork.getId());
            notification.setRefId(artwork.getId());
            
            log.info("Creating artwork rejected notification for user: {}, artwork: {}", owner.getId(), artwork.getId());
            Notification saved = notificationService.addNotification(notification);
            log.info("Artwork rejected notification created successfully with ID: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to send artwork rejected notification to user {}: {}", owner.getId(), e.getMessage(), e);
            // Không throw exception ở đây vì đây là notification phụ, không ảnh hưởng đến flow chính
        }
    }

    private void sendRoomClosedNotification(User admin, AuctionRoom room, String reportType, String reason) {
        try {
            Notification notification = new Notification();
            notification.setUserId(admin.getId());
            notification.setNotificationType(4); // Room closed type
            notification.setTitle("Phòng đấu giá đã bị đóng");
            String closedMessage = String.format("Phòng đấu giá '%s' đã bị đóng do: %s", room.getRoomName(), reportType);
            if (StringUtils.hasText(reason)) {
                closedMessage += String.format(". Lý do: %s", reason);
            }
            notification.setNotificationContent(closedMessage);
            notification.setNotificationStatus(1);
            notification.setLink("/auction-rooms/" + room.getId());
            notification.setRefId(room.getId());
            
            log.info("Creating room closed notification for user: {}, room: {}", admin.getId(), room.getId());
            Notification saved = notificationService.addNotification(notification);
            log.info("Room closed notification created successfully with ID: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to send room closed notification to user {}: {}", admin.getId(), e.getMessage(), e);
            // Không throw exception ở đây vì đây là notification phụ, không ảnh hưởng đến flow chính
        }
    }

    // ========== Email Methods ==========

    private void sendUserWarningEmail(User user, String reportType, String reason, String adminNote, long reportCount) {
        if (!StringUtils.hasText(user.getEmail())) {
            log.warn("User {} has no email, skip sending warning email", user.getId());
            return;
        }

        try {
            emailService.sendUserWarningEmail(user.getEmail(), 
                StringUtils.hasText(user.getUsername()) ? user.getUsername() : "Bạn",
                reportType, reason, adminNote, reportCount);
        } catch (Exception e) {
            log.error("Failed to send warning email to user {}", user.getEmail(), e);
        }
    }

    private void sendUserBlockedEmail(User user, String reportType, String reason, String adminNote) {
        if (!StringUtils.hasText(user.getEmail())) {
            log.warn("User {} has no email, skip sending blocked email", user.getId());
            return;
        }

        try {
            emailService.sendUserBlockedEmail(user.getEmail(), 
                StringUtils.hasText(user.getUsername()) ? user.getUsername() : "Bạn",
                reportType, reason, adminNote);
        } catch (Exception e) {
            log.error("Failed to send blocked email to user {}", user.getEmail(), e);
        }
    }

    private void sendArtworkRejectedByReportEmail(User owner, Artwork artwork, String reportType, String reason, String adminNote) {
        if (!StringUtils.hasText(owner.getEmail())) {
            log.warn("Owner {} has no email, skip sending artwork rejected email", owner.getId());
            return;
        }

        try {
            emailService.sendArtworkRejectedByReportEmail(owner.getEmail(), 
                StringUtils.hasText(owner.getUsername()) ? owner.getUsername() : "Bạn",
                artwork, reportType, reason, adminNote);
        } catch (Exception e) {
            log.error("Failed to send artwork rejected email to owner {}", owner.getEmail(), e);
        }
    }

    private void sendRoomClosedEmail(User admin, AuctionRoom room, String reportType, String reason, String adminNote) {
        if (!StringUtils.hasText(admin.getEmail())) {
            log.warn("Admin {} has no email, skip sending room closed email", admin.getId());
            return;
        }

        try {
            emailService.sendRoomClosedEmail(admin.getEmail(), 
                StringUtils.hasText(admin.getUsername()) ? admin.getUsername() : "Bạn",
                room, reportType, reason, adminNote);
        } catch (Exception e) {
            log.error("Failed to send room closed email to admin {}", admin.getEmail(), e);
        }
    }

    public ResponseEntity<AdminReportApiResponse<AdminReportStatisticsResponse>> getReportStatistics() {
        long total = reportsRepository.count();
        long pending = reportsRepository.countByReportStatus(0);
        long investigating = reportsRepository.countByReportStatus(1);
        long resolved = reportsRepository.countByReportStatus(2);

        // Lấy thống kê so sánh tháng này vs tháng trước
        MonthlyComparisonResponse monthlyComparison = monthlyStatisticsService.getMonthlyComparison("reports", "createdAt");
        MonthlyComparisonResponse.MonthlyComparisonData compData = monthlyComparison.getData();
        
        // Tạo monthly comparison data
        AdminReportStatisticsResponse.MonthlyComparison monthlyComp = new AdminReportStatisticsResponse.MonthlyComparison(
            compData.getCurrentMonth().getTotal(),
            compData.getPreviousMonth().getTotal(),
            compData.getChange().getAmount(),
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease(),
            compData.getCurrentMonth().getMonth(),
            compData.getPreviousMonth().getMonth()
        );

        AdminReportStatisticsResponse stats = new AdminReportStatisticsResponse(total, pending, investigating, resolved, monthlyComp);
        return ResponseEntity.ok(new AdminReportApiResponse<>(1, "Thống kê báo cáo", stats));
    }

    /**
     * Thống kê so sánh tháng này vs tháng trước cho reports
     */
    public ResponseEntity<MonthlyComparisonResponse> getReportMonthlyComparison() {
        MonthlyComparisonResponse response = monthlyStatisticsService.getMonthlyComparison("reports", "createdAt");
        return ResponseEntity.ok(response);
    }

    /**
     * Map Document từ MongoDB (có DBRef) sang AdminReportResponse
     */
    private AdminReportResponse mapDocumentToResponse(Document doc) {
        String reportId = doc.getString("_id");
        
        // ========== LẤY THÔNG TIN NGƯỜI BÁO CÁO (REPORTER) ==========
        // Ưu tiên: reporterId (field mới) -> userId (field cũ) -> user (DBRef)
        User reporter = null;
        String reporterId = null;
        
        // 1. Thử đọc từ reporterId (field mới)
        Object reporterIdObj = doc.get("reporterId");
        if (reporterIdObj != null) {
            reporterId = reporterIdObj.toString();
            if (reporterId != null && !reporterId.trim().isEmpty()) {
                reporter = userRepository.findById(reporterId).orElse(null);
            }
        }
        
        // 2. Nếu chưa có, thử đọc từ userId (field cũ)
        if (reporter == null) {
            Object userIdObj = doc.get("userId");
            if (userIdObj != null) {
                reporterId = userIdObj.toString();
                if (reporterId != null && !reporterId.trim().isEmpty()) {
                    reporter = userRepository.findById(reporterId).orElse(null);
                }
            }
        }
        
        // 3. Nếu vẫn chưa có, thử đọc từ user DBRef (cấu trúc cũ)
        if (reporter == null) {
            Object userRef = doc.get("user");
            if (userRef instanceof Document) {
                Document userDbRef = (Document) userRef;
                Object idObj = userDbRef.get("$id");
                if (idObj != null) {
                    reporterId = idObj.toString();
                    if (reporterId != null && !reporterId.trim().isEmpty()) {
                        reporter = userRepository.findById(reporterId).orElse(null);
                    }
                }
            } else if (userRef instanceof String) {
                reporterId = (String) userRef;
                if (reporterId != null && !reporterId.trim().isEmpty()) {
                    reporter = userRepository.findById(reporterId).orElse(null);
                }
            }
        }
        
        // ========== LẤY THÔNG TIN ĐỐI TƯỢNG BỊ BÁO CÁO ==========
        // Ưu tiên: reportedEntityId (field mới) -> objectId (field cũ) -> reportObject (DBRef)
        String objectId = null;
        String objectName = null;
        String objectEmail = null;
        User objectUser = null;
        Artwork objectArtwork = null;
        AuctionRoom objectRoom = null;
        
        // 1. Thử đọc từ reportedEntityId (field mới) và entityType
        Object reportedEntityIdObj = doc.get("reportedEntityId");
        Object entityTypeObj = doc.get("entityType");
        int entityType = 0;
        if (entityTypeObj instanceof Number) {
            entityType = ((Number) entityTypeObj).intValue();
        } else if (entityTypeObj instanceof String) {
            try {
                entityType = Integer.parseInt((String) entityTypeObj);
            } catch (NumberFormatException e) {
                entityType = 0;
            }
        }
        
        if (reportedEntityIdObj != null && entityType > 0) {
            objectId = reportedEntityIdObj.toString();
            if (objectId != null && !objectId.trim().isEmpty()) {
                // Lấy thông tin đối tượng dựa trên entityType
                switch (entityType) {
                    case ReportConstants.ENTITY_USER:
                        objectUser = userRepository.findById(objectId).orElse(null);
                        if (objectUser != null) {
                            objectName = objectUser.getUsername();
                            objectEmail = objectUser.getEmail();
                        }
                        break;
                    case ReportConstants.ENTITY_ARTWORK:
                    case ReportConstants.ENTITY_AI_ARTWORK:
                        objectArtwork = artworkRepository.findById(objectId).orElse(null);
                        if (objectArtwork != null) {
                            objectName = objectArtwork.getTitle();
                            // Lấy email của owner
                            if (objectArtwork.getOwnerId() != null) {
                                Optional<User> ownerOpt = userRepository.findById(objectArtwork.getOwnerId());
                                if (ownerOpt.isPresent()) {
                                    objectEmail = ownerOpt.get().getEmail();
                                }
                            }
                        }
                        break;
                    case ReportConstants.ENTITY_AUCTION_ROOM:
                        objectRoom = auctionRoomRepository.findById(objectId).orElse(null);
                        if (objectRoom != null) {
                            objectName = objectRoom.getRoomName();
                            // Lấy email của admin phụ trách
                            if (objectRoom.getAdminId() != null) {
                                Optional<User> adminOpt = userRepository.findById(objectRoom.getAdminId());
                                if (adminOpt.isPresent()) {
                                    objectEmail = adminOpt.get().getEmail();
                                }
                            }
                        }
                        break;
                }
            }
        }
        
        // 2. Nếu chưa có, thử đọc từ objectId (field cũ)
        if (objectId == null || objectName == null) {
            Object objectIdObj = doc.get("objectId");
            if (objectIdObj != null) {
                objectId = objectIdObj.toString();
                if (objectId != null && !objectId.trim().isEmpty()) {
                    // Thử tìm như User trước
                    objectUser = userRepository.findById(objectId).orElse(null);
                    if (objectUser != null) {
                        objectName = objectUser.getUsername();
                        objectEmail = objectUser.getEmail();
                    } else {
                        // Thử tìm như Artwork
                        objectArtwork = artworkRepository.findById(objectId).orElse(null);
                        if (objectArtwork != null) {
                            objectName = objectArtwork.getTitle();
                            if (objectArtwork.getOwnerId() != null) {
                                Optional<User> ownerOpt = userRepository.findById(objectArtwork.getOwnerId());
                                if (ownerOpt.isPresent()) {
                                    objectEmail = ownerOpt.get().getEmail();
                                }
                            }
                        } else {
                            // Thử tìm như AuctionRoom
                            objectRoom = auctionRoomRepository.findById(objectId).orElse(null);
                            if (objectRoom != null) {
                                objectName = objectRoom.getRoomName();
                                if (objectRoom.getAdminId() != null) {
                                    Optional<User> adminOpt = userRepository.findById(objectRoom.getAdminId());
                                    if (adminOpt.isPresent()) {
                                        objectEmail = adminOpt.get().getEmail();
                                    }
                                }
                            } else {
                                // Fallback về object field (String)
                                Object objectObj = doc.get("object");
                                if (objectObj != null) {
                                    objectName = objectObj.toString();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 3. Nếu vẫn chưa có, thử đọc từ reportObject DBRef (cấu trúc cũ)
        if (objectName == null) {
            Object reportObjectRef = doc.get("reportobject");
            if (reportObjectRef instanceof Document) {
                Document objectDbRef = (Document) reportObjectRef;
                Object idObj = objectDbRef.get("$id");
                if (idObj != null) {
                    String reportObjectId = idObj.toString();
                    if (reportObjectId != null && !reportObjectId.trim().isEmpty()) {
                        Optional<ReportObject> reportObjectOpt = reportObjectRepository.findById(reportObjectId);
                        if (reportObjectOpt.isPresent()) {
                            ReportObject reportObject = reportObjectOpt.get();
                            if (reportObject.getUserId() != null && !reportObject.getUserId().trim().isEmpty()) {
                                objectUser = userRepository.findById(reportObject.getUserId()).orElse(null);
                                if (objectUser != null) {
                                    objectName = objectUser.getUsername();
                                    objectEmail = objectUser.getEmail();
                                }
                            } else if (reportObject.getArtworkId() != null && !reportObject.getArtworkId().trim().isEmpty()) {
                                objectId = reportObject.getArtworkId();
                                objectArtwork = artworkRepository.findById(objectId).orElse(null);
                                if (objectArtwork != null) {
                                    objectName = objectArtwork.getTitle();
                                    if (objectArtwork.getOwnerId() != null) {
                                        Optional<User> ownerOpt = userRepository.findById(objectArtwork.getOwnerId());
                                        if (ownerOpt.isPresent()) {
                                            objectEmail = ownerOpt.get().getEmail();
                                        }
                                    }
                                } else {
                                    objectName = "Artwork: " + reportObject.getArtworkId();
                                }
                            } else if (reportObject.getAuctionRoomId() != null && !reportObject.getAuctionRoomId().trim().isEmpty()) {
                                objectId = reportObject.getAuctionRoomId();
                                objectRoom = auctionRoomRepository.findById(objectId).orElse(null);
                                if (objectRoom != null) {
                                    objectName = objectRoom.getRoomName();
                                    if (objectRoom.getAdminId() != null) {
                                        Optional<User> adminOpt = userRepository.findById(objectRoom.getAdminId());
                                        if (adminOpt.isPresent()) {
                                            objectEmail = adminOpt.get().getEmail();
                                        }
                                    }
                                } else {
                                    objectName = "AuctionRoom: " + reportObject.getAuctionRoomId();
                                }
                            }
                        }
                    }
                }
            } else if (reportObjectRef instanceof String) {
                String reportObjectId = (String) reportObjectRef;
                if (reportObjectId != null && !reportObjectId.trim().isEmpty()) {
                    Optional<ReportObject> reportObjectOpt = reportObjectRepository.findById(reportObjectId);
                    if (reportObjectOpt.isPresent()) {
                        ReportObject reportObject = reportObjectOpt.get();
                        if (reportObject.getUserId() != null && !reportObject.getUserId().trim().isEmpty()) {
                            objectUser = userRepository.findById(reportObject.getUserId()).orElse(null);
                            if (objectUser != null) {
                                objectName = objectUser.getUsername();
                                objectEmail = objectUser.getEmail();
                            }
                        } else if (reportObject.getArtworkId() != null && !reportObject.getArtworkId().trim().isEmpty()) {
                            objectId = reportObject.getArtworkId();
                            objectArtwork = artworkRepository.findById(objectId).orElse(null);
                            if (objectArtwork != null) {
                                objectName = objectArtwork.getTitle();
                                if (objectArtwork.getOwnerId() != null) {
                                    Optional<User> ownerOpt = userRepository.findById(objectArtwork.getOwnerId());
                                    if (ownerOpt.isPresent()) {
                                        objectEmail = ownerOpt.get().getEmail();
                                    }
                                }
                            } else {
                                objectName = "Artwork: " + reportObject.getArtworkId();
                            }
                        } else if (reportObject.getAuctionRoomId() != null && !reportObject.getAuctionRoomId().trim().isEmpty()) {
                            objectId = reportObject.getAuctionRoomId();
                            objectRoom = auctionRoomRepository.findById(objectId).orElse(null);
                            if (objectRoom != null) {
                                objectName = objectRoom.getRoomName();
                                if (objectRoom.getAdminId() != null) {
                                    Optional<User> adminOpt = userRepository.findById(objectRoom.getAdminId());
                                    if (adminOpt.isPresent()) {
                                        objectEmail = adminOpt.get().getEmail();
                                    }
                                }
                            } else {
                                objectName = "AuctionRoom: " + reportObject.getAuctionRoomId();
                            }
                        }
                    }
                }
            }
        }
        
        // ========== LẤY CÁC TRƯỜNG KHÁC ==========
        // reportTarget: từ reportTarget hoặc xác định từ entityType
        String reportTarget = doc.getString("reportTarget");
        if (reportTarget == null && entityType > 0) {
            reportTarget = ReportConstants.getEntityTypeName(entityType);
        }
        
        // reportReason: ưu tiên reason (field mới) -> reportReason (field cũ)
        String reportReason = doc.getString("reason");
        if (reportReason == null || reportReason.trim().isEmpty()) {
            reportReason = doc.getString("reportReason");
        }
        
        // status: ưu tiên status (field mới) -> reportStatus (field cũ)
        Object statusObj = doc.get("status");
        if (statusObj == null) {
            statusObj = doc.get("reportStatus");
        }
        int reportStatus = 0;
        if (statusObj instanceof Number) {
            reportStatus = ((Number) statusObj).intValue();
        } else if (statusObj instanceof String) {
            try {
                reportStatus = Integer.parseInt((String) statusObj);
            } catch (NumberFormatException e) {
                reportStatus = 0;
            }
        }
        
        // Lấy thời gian
        LocalDateTime reportTime = null;
        Object reportTimeObj = doc.get("reportTime");
        if (reportTimeObj instanceof Date) {
            reportTime = ((Date) reportTimeObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (reportTimeObj instanceof Document) {
            Date date = ((Document) reportTimeObj).getDate("$date");
            if (date != null) {
                reportTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        
        LocalDateTime createdAt = null;
        Object createdAtObj = doc.get("createdAt");
        if (createdAtObj instanceof Date) {
            createdAt = ((Date) createdAtObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (createdAtObj instanceof Document) {
            Date date = ((Document) createdAtObj).getDate("$date");
            if (date != null) {
                createdAt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        
        LocalDateTime reportDoneTime = null;
        // Ưu tiên đọc từ resolvedAt (field trong entity), fallback về reportDoneTime (field cũ trong MongoDB)
        Object resolvedAtObj = doc.get("resolvedAt");
        if (resolvedAtObj instanceof Date) {
            reportDoneTime = ((Date) resolvedAtObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (resolvedAtObj instanceof Document) {
            Date date = ((Document) resolvedAtObj).getDate("$date");
            if (date != null) {
                reportDoneTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        // Nếu không có resolvedAt, đọc từ reportDoneTime (field cũ)
        if (reportDoneTime == null) {
            Object reportDoneTimeObj = doc.get("reportDoneTime");
            if (reportDoneTimeObj instanceof Date) {
                reportDoneTime = ((Date) reportDoneTimeObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            } else if (reportDoneTimeObj instanceof Document) {
                Date date = ((Document) reportDoneTimeObj).getDate("$date");
                if (date != null) {
                    reportDoneTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        }
        
        // Nếu reportTime null, dùng createdAt
        if (reportTime == null) {
            reportTime = createdAt;
        }

        return new AdminReportResponse(
                reportId,
                reporterId,
                reporter != null ? reporter.getUsername() : null,
                reporter != null ? reporter.getEmail() : null,
                reporter != null ? reporter.getAvt() : null,
                objectId,
                objectName,
                objectEmail, // Dùng objectEmail đã lấy được từ các nguồn khác nhau
                reportTarget,
                reportReason,
                reportStatus,
                reportTime,
                createdAt,
                reportDoneTime
        );
    }

    /**
     * Map Reports entity sang AdminReportResponse (fallback nếu không dùng DBRef)
     */
    private AdminReportResponse mapToResponse(Reports report) {
        // ========== LẤY THÔNG TIN NGƯỜI BÁO CÁO ==========
        // Ưu tiên: reporterId (field mới) -> userId (field cũ)
        User reporter = null;
        String reporterId = null;
        
        if (report.getReporterId() != null && !report.getReporterId().trim().isEmpty()) {
            reporterId = report.getReporterId();
            reporter = userRepository.findById(reporterId).orElse(null);
        } else if (report.getUserId() != null && !report.getUserId().trim().isEmpty()) {
            reporterId = report.getUserId();
            reporter = userRepository.findById(reporterId).orElse(null);
        }
        
        // ========== LẤY THÔNG TIN ĐỐI TƯỢNG BỊ BÁO CÁO ==========
        // Ưu tiên: reportedEntityId (field mới) -> objectId (field cũ)
        String objectId = null;
        String objectName = null;
        String objectEmail = null;
        
        if (report.getReportedEntityId() != null && !report.getReportedEntityId().trim().isEmpty() && report.getEntityType() > 0) {
            objectId = report.getReportedEntityId();
            // Lấy thông tin đối tượng dựa trên entityType
            switch (report.getEntityType()) {
                case ReportConstants.ENTITY_USER:
                    Optional<User> userOpt = userRepository.findById(objectId);
                    if (userOpt.isPresent()) {
                        User objectUser = userOpt.get();
                        objectName = objectUser.getUsername();
                        objectEmail = objectUser.getEmail();
                    }
                    break;
                case ReportConstants.ENTITY_ARTWORK:
                case ReportConstants.ENTITY_AI_ARTWORK:
                    Optional<Artwork> artworkOpt = artworkRepository.findById(objectId);
                    if (artworkOpt.isPresent()) {
                        Artwork artwork = artworkOpt.get();
                        objectName = artwork.getTitle();
                        if (artwork.getOwnerId() != null) {
                            Optional<User> ownerOpt = userRepository.findById(artwork.getOwnerId());
                            if (ownerOpt.isPresent()) {
                                objectEmail = ownerOpt.get().getEmail();
                            }
                        }
                    }
                    break;
                case ReportConstants.ENTITY_AUCTION_ROOM:
                    Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(objectId);
                    if (roomOpt.isPresent()) {
                        AuctionRoom room = roomOpt.get();
                        objectName = room.getRoomName();
                        if (room.getAdminId() != null) {
                            Optional<User> adminOpt = userRepository.findById(room.getAdminId());
                            if (adminOpt.isPresent()) {
                                objectEmail = adminOpt.get().getEmail();
                            }
                        }
                    }
                    break;
            }
        } else if (report.getObjectId() != null && !report.getObjectId().trim().isEmpty()) {
            objectId = report.getObjectId();
            // Thử tìm như User trước
            Optional<User> userOpt = userRepository.findById(objectId);
            if (userOpt.isPresent()) {
                User objectUser = userOpt.get();
                objectName = objectUser.getUsername();
                objectEmail = objectUser.getEmail();
            } else {
                // Thử tìm như Artwork
                Optional<Artwork> artworkOpt = artworkRepository.findById(objectId);
                if (artworkOpt.isPresent()) {
                    Artwork artwork = artworkOpt.get();
                    objectName = artwork.getTitle();
                    if (artwork.getOwnerId() != null) {
                        Optional<User> ownerOpt = userRepository.findById(artwork.getOwnerId());
                        if (ownerOpt.isPresent()) {
                            objectEmail = ownerOpt.get().getEmail();
                        }
                    }
                } else {
                    // Thử tìm như AuctionRoom
                    Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(objectId);
                    if (roomOpt.isPresent()) {
                        AuctionRoom room = roomOpt.get();
                        objectName = room.getRoomName();
                        if (room.getAdminId() != null) {
                            Optional<User> adminOpt = userRepository.findById(room.getAdminId());
                            if (adminOpt.isPresent()) {
                                objectEmail = adminOpt.get().getEmail();
                            }
                        }
                    } else {
                        // Fallback về object field (String)
                        objectName = report.getObject();
                    }
                }
            }
        }
        
        // reportTarget: từ entityType hoặc null
        String reportTarget = null;
        if (report.getEntityType() > 0) {
            reportTarget = ReportConstants.getEntityTypeName(report.getEntityType());
        }
        
        // reportReason: ưu tiên reason (field mới) -> reportReason (field cũ)
        String reportReason = report.getReason();
        if (reportReason == null || reportReason.trim().isEmpty()) {
            reportReason = report.getReportReason();
        }
        
        // status: ưu tiên status (field mới) -> reportStatus (field cũ)
        int reportStatus = report.getStatus();
        if (reportStatus == 0 && report.getReportStatus() != 0) {
            reportStatus = report.getReportStatus();
        }

        return new AdminReportResponse(
                report.getId(),
                reporterId,
                reporter != null ? reporter.getUsername() : null,
                reporter != null ? reporter.getEmail() : null,
                reporter != null ? reporter.getAvt() : null,
                objectId,
                objectName,
                objectEmail,
                reportTarget,
                reportReason,
                reportStatus,
                report.getCreatedAt(), // reportTime
                report.getCreatedAt(),
                report.getResolvedAt()
        );
    }
}

