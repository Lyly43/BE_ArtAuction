package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportResponse {
    private String id;
    private String reporterId;
    private String reporterName;
    private String reporterEmail;
    private String objectId;
    private String objectName;
    private String objectEmail;
    private String reportTarget; // "User", "Artwork", "AuctionRoom", etc.
    private String reportReason;
    private int reportStatus;
    private LocalDateTime reportTime; // Thời gian báo cáo (createdAt hoặc reportTime từ DB)
    private LocalDateTime createdAt;
    private LocalDateTime reportDoneTime;
}


