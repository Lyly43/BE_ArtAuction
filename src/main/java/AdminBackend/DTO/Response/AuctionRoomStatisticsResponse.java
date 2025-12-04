package AdminBackend.DTO.Response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuctionRoomStatisticsResponse {
    private long totalRooms;
    private long runningRooms;    // status = 1
    private long upcomingRooms;   // status = 0
    private long completedRooms;  // status = 2
    private long cancelRooms;     // status = 3 (hoãn)
    
    // Thống kê so sánh tháng này vs tháng trước cho totalRooms
    private MonthlyComparison monthlyComparison;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyComparison {
        private long currentMonth; // Số phòng tháng này
        private long previousMonth; // Số phòng tháng trước
        private long changeAmount; // Số thay đổi (có thể âm)
        private double changePercentage; // Phần trăm thay đổi (có thể âm)
        private boolean isIncrease; // true nếu tăng, false nếu giảm
        private String currentMonthLabel; // Format: "MM/yyyy"
        private String previousMonthLabel; // Format: "MM/yyyy"
    }
}

