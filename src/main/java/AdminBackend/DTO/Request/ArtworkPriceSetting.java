package AdminBackend.DTO.Request;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ArtworkPriceSetting {
    private String artworkId; // ID của tác phẩm
    private BigDecimal startingPrice; // Giá khởi điểm
    private BigDecimal bidStep; // Bước giá tối thiểu
}

