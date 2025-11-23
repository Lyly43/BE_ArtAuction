package AdminBackend.DTO.Response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminArtworkResponse {
    private String id;
    private String title;
    private String author; // Lấy từ username của ownerId
    private int yearOfCreation;
    private String material;
    private String paintingGenre;
    private String size;
    private String avtArtwork;
    private BigDecimal startedPrice;
    private int status; // 0: Chưa duyệt, 1: Đã duyệt, 2: Đang đấu giá, 3: Từ chối
    private LocalDateTime createdAt;
}

