package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAuthResponse {
    private boolean success;
    private String message;
    private String token;
    private String adminId;
    private String fullName;
    private String email;
    private String role;
    private String avatar;
}


