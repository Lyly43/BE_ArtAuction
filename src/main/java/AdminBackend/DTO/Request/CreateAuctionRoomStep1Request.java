package AdminBackend.DTO.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateAuctionRoomStep1Request {
    private String roomName;
    private String description;
    private String material; // Filter material của artwork (có thể dùng để filter sau)
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;
    
    private String adminId; // ID của admin phụ trách
    private String adminName; // Tên admin phụ trách (optional, có thể lấy từ adminId)
}

