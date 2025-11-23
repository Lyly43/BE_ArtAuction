package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddNotificationRequest {
    private String userId;
    private Integer notificationType;
    private String title;
    private String link;
    private String notificationContent;
    private Integer notificationStatus;
    private LocalDateTime notificationTime;
    private String refId;
}

