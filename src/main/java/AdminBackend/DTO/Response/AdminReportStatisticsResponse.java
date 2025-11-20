package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportStatisticsResponse {
    private long totalReports;
    private long pendingReports;
    private long investigatingReports;
    private long resolvedReports;
}

