package AdminBackend.DTO.Response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserStatisticsResponse {
    private long totalUsers; // Tổng người dùng
    private long activeUsers; // Tổng người dùng đang hoạt động (status = 1)
    private long totalSellers; // Tổng người bán (role = 3)
    private long totalBlockedUsers; // Tổng người dùng bị khóa (status = 2)
    
    // Thống kê so sánh tháng này vs tháng trước cho totalUsers
    private MonthlyComparison monthlyComparison;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyComparison {
        private long currentMonth; // Số user tháng này
        private long previousMonth; // Số user tháng trước
        private long changeAmount; // Số thay đổi (có thể âm)
        private double changePercentage; // Phần trăm thay đổi (có thể âm)
        private boolean isIncrease; // true nếu tăng, false nếu giảm
        private String currentMonthLabel; // Format: "MM/yyyy"
        private String previousMonthLabel; // Format: "MM/yyyy"
    }
}

