package AdminBackend.DTO.Request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AddAdminRequest {
    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    private String address;
    private int status; // 0 = Bị Khóa, 1 = Hoạt động
}


