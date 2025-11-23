package AdminBackend.DTO.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateAuctionRoomRequest {
    private String roomName;
    private String description;
    private String type;
    private String imageAuctionRoom;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime stoppedAt;

    private Integer viewCount;
}

