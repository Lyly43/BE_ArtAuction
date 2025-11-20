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
}

