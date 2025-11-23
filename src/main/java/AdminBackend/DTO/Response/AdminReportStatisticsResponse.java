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
    
    // Thống kê so sánh tháng này vs tháng trước cho totalReports
    private MonthlyComparison monthlyComparison;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyComparison {
        private long currentMonth; // Số report tháng này
        private long previousMonth; // Số report tháng trước
        private long changeAmount; // Số thay đổi (có thể âm)
        private double changePercentage; // Phần trăm thay đổi (có thể âm)
        private boolean isIncrease; // true nếu tăng, false nếu giảm
        private String currentMonthLabel; // Format: "MM/yyyy"
        private String previousMonthLabel; // Format: "MM/yyyy"
    }
}


