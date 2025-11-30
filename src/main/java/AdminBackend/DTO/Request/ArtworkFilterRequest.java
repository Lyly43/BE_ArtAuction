package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkFilterRequest {
    // Genre filter: null = All, hoặc giá trị như "Abstract", "Portrait", "Landscape", "Modern", "Traditional"
    private String paintingGenre;
    
    // Price range filter - có thể dùng preset hoặc custom range
    // Preset: "<5tr", "5-20tr", "20-100tr", ">100tr", hoặc null
    private String priceRange;
    
    // Custom price range (nếu không dùng preset)
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    
    // Status filter: null = All, 0 = Not Approved, 1 = Approved, 2 = Up for Auction, 3 = Refused
    private Integer status;
}


