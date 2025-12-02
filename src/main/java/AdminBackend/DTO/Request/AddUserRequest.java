package AdminBackend.DTO.Request;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AddUserRequest {
    private String username;
    private String password;
    private String email;
    private String phonenumber;
    private String cccd;
    private String address;
    private LocalDate dateOfBirth;
    private Integer gender; // 0 = male, 1 = female, 2 = other
    private int role; // 1 = normal user, 2 = buyer, 3 = seller
    private int status; // 1 = active, 0 = offline, 2 = blocked
    private String avt; // Avatar URL
}

