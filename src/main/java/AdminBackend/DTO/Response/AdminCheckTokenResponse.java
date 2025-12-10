package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCheckTokenResponse {
    private int status;
    private String message;
    private String name;
    private String email;
    private String avatar;
    private Integer role;
}

