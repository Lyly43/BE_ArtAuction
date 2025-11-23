package AdminBackend.DTO.Request;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AddArtworkRequest {
    private String title;
    private String author; // ownerId - sẽ lấy từ User
    private String description;
    private String size;
    private String material;
    private String paintingGenre; // painting genres
    private BigDecimal startedPrice;
    private String avtArtwork; // URL của ảnh đại diện
    private List<String> imageUrls; // Danh sách URL ảnh
    private int yearOfCreation;
    private String ownerId; // ID của user sở hữu tác phẩm
}

