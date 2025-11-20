package AdminBackend.DTO.Response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ArtworkStatisticsResponse {
    private long totalArtworks; // Tổng tác phẩm
    private long pendingArtworks; // Chưa duyệt (status = 0)
    private long approvedArtworks; // Đã duyệt (status = 1)
    private long rejectedArtworks; // Từ chối (status = 3)
}

