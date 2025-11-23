package AdminBackend.DTO.Request;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateArtworkRequest {
    private String title;
    private String description;
    private String size;
    private String material;
    private String paintingGenre;
    private BigDecimal startedPrice;
    private String avtArtwork;
    private List<String> imageUrls;
    private int yearOfCreation;
    private int status; // 0: Chưa duyệt, 1: Đã duyệt, 2: Đang đấu giá, 3: Từ chối
    private String ownerId; // Có thể đổi owner
}

