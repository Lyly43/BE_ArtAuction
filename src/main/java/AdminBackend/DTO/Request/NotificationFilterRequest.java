package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFilterRequest {
    // Notification Status: null = bỏ qua filter (lấy tất cả)
    // 0 = failed, 1 = sent
    private Integer notificationStatus;
    
    // Date range: Lọc theo notificationTime của notification
    private LocalDateTime dateFrom; // null = bỏ qua filter
    private LocalDateTime dateTo;   // null = bỏ qua filter
}

