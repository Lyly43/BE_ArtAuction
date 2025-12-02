package AdminBackend.DTO.Response;

import lombok.*;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ArtworkForSelectionResponse {
    private String id;
    private String title;
    private String author; // Lấy từ username của ownerId
    private String description;
    private String paintingGenre; // Thể loại
    private String material;
    private String size;
    private String avtArtwork;
    private BigDecimal startedPrice; // Giá khởi điểm hiện tại của artwork
    private int status; // Status của artwork
}

