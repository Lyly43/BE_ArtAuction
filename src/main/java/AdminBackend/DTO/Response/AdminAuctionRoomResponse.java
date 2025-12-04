package AdminBackend.DTO.Response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminAuctionRoomResponse {
    private String id;
    private Integer viewCount;
    private String roomName;
    private String description;
    private String type;
    private String imageAuctionRoom;
    // 0: Sắp diễn ra, 1: Đang diễn ra, 2: Đã hoàn thành, 3: Hoãn
    private int status;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    // Thời gian kết thúc dự kiến
    private LocalDateTime estimatedEndTime;
    private LocalDateTime createdAt;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private Integer totalMembers; // Tổng số người tham gia (tổng số user trong memberIds)
}

