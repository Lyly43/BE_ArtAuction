package AdminBackend.DTO.Response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ArtworkStatisticsResponse {
    private long totalArtworks; // Tổng tác phẩm
    private long pendingArtworks; // Chưa duyệt (status = 0)
    private long approvedArtworks; // Đã duyệt (status = 1)
    private long rejectedArtworks; // Từ chối (status = 3)
    
    // Thống kê so sánh tháng này vs tháng trước cho totalArtworks
    private MonthlyComparison monthlyComparison;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyComparison {
        private long currentMonth; // Số artwork tháng này
        private long previousMonth; // Số artwork tháng trước
        private long changeAmount; // Số thay đổi (có thể âm)
        private double changePercentage; // Phần trăm thay đổi (có thể âm)
        private boolean isIncrease; // true nếu tăng, false nếu giảm
        private String currentMonthLabel; // Format: "MM/yyyy"
        private String previousMonthLabel; // Format: "MM/yyyy"
    }
}

