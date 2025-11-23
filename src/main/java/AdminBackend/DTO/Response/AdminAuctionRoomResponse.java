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
    private int status; // 0: Sắp diễn ra, 1: Đang diễn ra, 2: Đã hoàn thành
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private LocalDateTime createdAt;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private Integer totalMembers; // Tổng số người tham gia (tổng số user trong memberIds)
}

