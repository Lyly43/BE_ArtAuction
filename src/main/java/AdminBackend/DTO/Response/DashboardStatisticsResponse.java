package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatisticsResponse {
    private int status;
    private String message;
    private StatisticsData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsData {
        // Thống kê có so sánh tháng
        private MonthlyStat totalUsers;
        private MonthlyStat totalArtworks;
        private MonthlyStat totalBids;
        private MonthlyRevenueStat revenue;

        // Thống kê đơn giản (không so sánh)
        private long totalAuctionRooms;
        private long activeUsers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStat {
        private long currentMonth;
        private long previousMonth;
        private long change;
        private double percentage;
        private boolean isIncrease;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueStat {
        private BigDecimal currentMonth;
        private BigDecimal previousMonth;
        private BigDecimal change;
        private double percentage;
        private boolean isIncrease;
    }
}

