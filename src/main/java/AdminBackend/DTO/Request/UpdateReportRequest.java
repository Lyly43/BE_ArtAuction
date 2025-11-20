package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportRequest {
    private String reportReason;
    private Integer reportStatus;
    private LocalDateTime reportDoneTime;
}

