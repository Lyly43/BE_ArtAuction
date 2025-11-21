package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationStatisticsResponse {
    private long totalNotifications;
    private Map<Integer, Long> statusCounts;
    private Double readRate;
}

