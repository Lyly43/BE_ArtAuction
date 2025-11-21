package AdminBackend.Controller;

import AdminBackend.DTO.Request.AddNotificationRequest;
import AdminBackend.DTO.Request.UpdateNotificationRequest;
import AdminBackend.DTO.Response.AdminNotificationApiResponse;
import AdminBackend.DTO.Response.AdminNotificationResponse;
import AdminBackend.DTO.Response.AdminNotificationStatisticsResponse;
import AdminBackend.Service.AdminNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminNotificationController {

    @Autowired
    private AdminNotificationService adminNotificationService;

    @GetMapping("/lay-du-lieu")
    public ResponseEntity<AdminNotificationApiResponse<List<AdminNotificationResponse>>> getAllNotifications() {
        return adminNotificationService.getAllNotifications();
    }

    @GetMapping("/tim-kiem")
    public ResponseEntity<AdminNotificationApiResponse<List<AdminNotificationResponse>>> searchNotifications(
            @RequestParam(value = "q", required = false) String searchTerm) {
        return adminNotificationService.searchNotifications(searchTerm);
    }

    @PostMapping("/tao-thong-bao")
    public ResponseEntity<AdminNotificationApiResponse<AdminNotificationResponse>> addNotification(
            @RequestBody AddNotificationRequest request) {
        return adminNotificationService.addNotification(request);
    }

    @PutMapping("/cap-nhat/{notificationId}")
    public ResponseEntity<AdminNotificationApiResponse<AdminNotificationResponse>> updateNotification(
            @PathVariable String notificationId,
            @RequestBody UpdateNotificationRequest request) {
        return adminNotificationService.updateNotification(notificationId, request);
    }

    @DeleteMapping("/xoa/{notificationId}")
    public ResponseEntity<AdminNotificationApiResponse<Void>> deleteNotification(@PathVariable String notificationId) {
        return adminNotificationService.deleteNotification(notificationId);
    }

    @GetMapping("/thong-ke")
    public ResponseEntity<AdminNotificationApiResponse<AdminNotificationStatisticsResponse>> getStatistics() {
        return adminNotificationService.getStatistics();
    }
}

