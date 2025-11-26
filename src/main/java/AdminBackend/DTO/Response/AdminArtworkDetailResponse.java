package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminArtworkDetailResponse {
    private String id;
    private String ownerId;
    private OwnerInfo owner;
    private String title;
    private String description;
    private String paintingGenre;
    private String material;
    private String size;
    private int yearOfCreation;
    private String certificateId;
    private BigDecimal startedPrice;
    private String avtArtwork;
    private List<String> imageUrls;
    private int status;
    private boolean aiVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OwnerInfo {
        private String id;
        private String username;
        private String email;
        private String phoneNumber;
        private int status;
    }
}

