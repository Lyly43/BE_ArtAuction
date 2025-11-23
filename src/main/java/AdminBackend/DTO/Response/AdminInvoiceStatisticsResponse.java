package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminInvoiceStatisticsResponse {
    private long totalInvoices;
    private long paidInvoices;
    private long pendingInvoices;
    private long failedInvoices;
    
    // Thống kê so sánh tháng này vs tháng trước cho số lượng invoice
    private MonthlyComparison monthlyComparison;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyComparison {
        private long currentMonth; // Số invoice tháng này
        private long previousMonth; // Số invoice tháng trước
        private long changeAmount; // Số thay đổi (có thể âm)
        private double changePercentage; // Phần trăm thay đổi (có thể âm)
        private boolean isIncrease; // true nếu tăng, false nếu giảm
        private String currentMonthLabel; // Format: "MM/yyyy"
        private String previousMonthLabel; // Format: "MM/yyyy"
    }
}

