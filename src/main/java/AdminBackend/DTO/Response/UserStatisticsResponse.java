package AdminBackend.DTO.Response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserStatisticsResponse {
    private long totalUsers; // Tổng người dùng
    private long totalSellers; // Tổng người bán (role = 3)
    private long totalBlockedUsers; // Tổng người dùng bị khóa (status = 2)
}

