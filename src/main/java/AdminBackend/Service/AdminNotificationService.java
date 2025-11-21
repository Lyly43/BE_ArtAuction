package AdminBackend.Service;

import AdminBackend.DTO.Request.AddNotificationRequest;
import AdminBackend.DTO.Request.UpdateNotificationRequest;
import AdminBackend.DTO.Response.AdminNotificationApiResponse;
import AdminBackend.DTO.Response.AdminNotificationResponse;
import AdminBackend.DTO.Response.AdminNotificationStatisticsResponse;
import com.auctionaa.backend.Entity.Notification;
import com.auctionaa.backend.Repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminNotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public ResponseEntity<AdminNotificationApiResponse<List<AdminNotificationResponse>>> getAllNotifications() {
        List<Notification> notifications = notificationRepository.findAll();
        List<AdminNotificationResponse> data = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AdminNotificationApiResponse<>(1, "Lấy danh sách thông báo thành công", data));
    }

    public ResponseEntity<AdminNotificationApiResponse<List<AdminNotificationResponse>>> searchNotifications(String searchTerm) {
        List<Notification> notifications;
        if (!StringUtils.hasText(searchTerm)) {
            notifications = notificationRepository.findAll();
        } else {
            notifications = notificationRepository.searchNotifications(searchTerm);
        }

        List<AdminNotificationResponse> data = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        String message = StringUtils.hasText(searchTerm)
                ? String.format("Tìm thấy %d thông báo cho từ khóa '%s'", data.size(), searchTerm)
                : "Lấy danh sách thông báo thành công";

        return ResponseEntity.ok(new AdminNotificationApiResponse<>(1, message, data));
    }

    public ResponseEntity<AdminNotificationApiResponse<AdminNotificationResponse>> addNotification(AddNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setNotificationType(request.getNotificationType() == null ? 0 : request.getNotificationType());
        notification.setTitle(request.getTitle());
        notification.setLink(request.getLink());
        notification.setNotificationContent(request.getNotificationContent());
        notification.setNotificationStatus(request.getNotificationStatus() == null ? 0 : request.getNotificationStatus());
        notification.setNotificationTime(request.getNotificationTime() != null ? request.getNotificationTime() : LocalDateTime.now());
        notification.setRefId(request.getRefId());
        notification.setCreatedAt(LocalDateTime.now());
        notification.generateId();

        Notification saved = notificationRepository.save(notification);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminNotificationApiResponse<>(1, "Tạo thông báo thành công", mapToResponse(saved)));
    }

    public ResponseEntity<AdminNotificationApiResponse<AdminNotificationResponse>> updateNotification(
            String notificationId,
            UpdateNotificationRequest request) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminNotificationApiResponse<>(0, "Không tìm thấy thông báo", null));
        }

        Notification notification = optionalNotification.get();

        if (request.getUserId() != null) {
            notification.setUserId(request.getUserId());
        }
        if (request.getNotificationType() != null) {
            notification.setNotificationType(request.getNotificationType());
        }
        if (StringUtils.hasText(request.getTitle())) {
            notification.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getLink())) {
            notification.setLink(request.getLink());
        }
        if (StringUtils.hasText(request.getNotificationContent())) {
            notification.setNotificationContent(request.getNotificationContent());
        }
        if (request.getNotificationStatus() != null) {
            notification.setNotificationStatus(request.getNotificationStatus());
        }
        if (request.getNotificationTime() != null) {
            notification.setNotificationTime(request.getNotificationTime());
        }
        if (request.getRefId() != null) {
            notification.setRefId(request.getRefId());
        }

        Notification saved = notificationRepository.save(notification);
        return ResponseEntity.ok(new AdminNotificationApiResponse<>(1, "Cập nhật thông báo thành công", mapToResponse(saved)));
    }

    public ResponseEntity<AdminNotificationApiResponse<Void>> deleteNotification(String notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminNotificationApiResponse<>(0, "Không tìm thấy thông báo", null));
        }

        notificationRepository.delete(optionalNotification.get());
        return ResponseEntity.ok(new AdminNotificationApiResponse<>(1, "Xóa thông báo thành công", null));
    }

    public ResponseEntity<AdminNotificationApiResponse<AdminNotificationStatisticsResponse>> getStatistics() {
        long total = notificationRepository.count();

        Map<Integer, Long> statusCounts = notificationRepository.findAll().stream()
                .collect(Collectors.groupingBy(Notification::getNotificationStatus, Collectors.counting()));

        Double readRate = null;
        Long sentCount = statusCounts.getOrDefault(1, 0L);
        if (total > 0) {
            readRate = (sentCount.doubleValue() / total) * 100.0;
        }

        AdminNotificationStatisticsResponse stats = new AdminNotificationStatisticsResponse(total, statusCounts, readRate);
        return ResponseEntity.ok(new AdminNotificationApiResponse<>(1, "Thống kê thông báo", stats));
    }

    private AdminNotificationResponse mapToResponse(Notification notification) {
        return new AdminNotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getLink(),
                notification.getNotificationContent(),
                notification.getNotificationStatus(),
                notification.getNotificationTime(),
                notification.getRefId(),
                notification.getCreatedAt()
        );
    }
}

