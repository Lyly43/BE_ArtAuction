package AdminBackend.DTO.Response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminAdminResponse {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String avatar;
    private int role; // Default: 3 (admin role)
    private Integer status; // 0 = Bị Khóa, 1 = Hoạt động
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


