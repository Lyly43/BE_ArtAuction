package AdminBackend.DTO.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateAuctionRoomCompleteRequest {
    // Bước 1: Thông tin cơ bản
    private String roomName;
    private String description;
    private String material; // Filter material
    private String imageAuctionRoom;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime stoppedAt; // Optional

    // Thời gian kết thúc dự kiến của cả phòng (optional)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedEndTime;
    
    private String adminId; // ID của admin phụ trách
    private String type; // Loại phòng đấu giá
    
    // Bước 3: Danh sách tác phẩm với giá
    private List<ArtworkPriceSetting> artworks; // Danh sách tác phẩm với startingPrice và bidStep
    
    // Bước 4: Cấu hình tài chính
    private BigDecimal depositAmount; // Tiền đặt cọc
    private Integer paymentDeadlineDays; // Thời hạn thanh toán sau khi thắng (số ngày)
}

