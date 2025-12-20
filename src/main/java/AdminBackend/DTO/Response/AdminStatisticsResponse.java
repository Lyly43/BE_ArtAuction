package AdminBackend.DTO.Response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminStatisticsResponse {
    private long totalAdmins;
    private long activeAdmins; // status = 1
    private long lockedAdmins; // status = 0
    private long totalSuperAdmins; // role = 4
}


