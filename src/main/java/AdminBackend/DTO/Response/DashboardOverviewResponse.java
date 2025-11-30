package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private int status;
    private String message;
    private DashboardData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardData {
        // Thống kê có so sánh tháng
        private MonthlyStat totalUsers;
        private MonthlyStat totalArtworks;
        private MonthlyStat totalBids;
        private MonthlyRevenueStat revenue;

        // Thống kê đơn giản (không so sánh)
        private long totalAuctionRooms;
        private long activeUsers;

        // Danh sách
        private List<AuctionRoomWithSessions> topAuctionRooms; // Top 10 mới nhất
        private List<NewUserInfo> topNewUsers; // Top 10 mới nhất
        private List<TopSessionInfo> topSessions; // Top 10 giá cao nhất
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionRoomWithSessions {
        private String id;
        private String roomName;
        private String description;
        private String imageAuctionRoom;
        private String type;
        private int status;
        private LocalDateTime startedAt;
        private LocalDateTime stoppedAt;
        private List<SessionInfo> sessions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private String id;
        private String artworkId;
        private String artworkTitle;
        private String avtArtwork;
        private BigDecimal startingPrice;
        private BigDecimal currentPrice;
        private int status;
        private Integer orderIndex;
        private LocalDateTime startTime;
        private LocalDateTime endedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewUserInfo {
        private String id;
        private String username;
        private String email;
        private String avt;
        private LocalDateTime createdAt;
        private int status; // 1 = active, 0 = offline, 2 = blocked
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSessionInfo {
        private String sessionId;
        private String auctionRoomId;
        private String roomName;
        private String artworkId;
        private String artworkTitle;
        private String artworkImageUrl;
        private String artistName;
        private String paintingGenre; // Thể loại tranh từ artwork
        private BigDecimal totalAmount;
        private String winnerName;
        private String winnerEmail;
        private LocalDateTime orderDate;
        private Integer viewCount; // Lượt xem của session
    }
}

