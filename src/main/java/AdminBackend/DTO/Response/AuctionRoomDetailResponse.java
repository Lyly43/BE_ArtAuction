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
public class AuctionRoomDetailResponse {
    private String id;
    private String roomName;
    private String type;
    private AdminInfo admin;
    private String description;
    private String imageAuctionRoom;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private Integer totalMembers;
    private Integer viewCount;
    private BigDecimal depositAmount;
    private int status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SessionArtworkInfo> artworks;

    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdminInfo {
        private String id;
        private String fullName;
        private String email;
        private String phoneNumber;
    }

    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionArtworkInfo {
        private String sessionId;
        private String artworkId;
        private String artworkName;
        private String author;
        private String avtArtwork;
        private BigDecimal startingPrice;
        private BigDecimal currentPrice;
        private BigDecimal bidStep;
        private int status;
        // Thông tin chi tiết từ Artwork
        private List<String> imageUrls;
        private boolean aiVerified;
        private String size;
        private String material;
        private String certificateId;
        private String paintingGenre;
        private int yearOfCreation;
        private String description;
    }
}



