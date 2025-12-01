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
    private Integer status; // 0 = Bị Khóa, 1 = Hoạt động, default: 1
    private Integer role;   // optional, default: 3
    private String avatar;  // URL avatar (lấy từ endpoint upload-avatar)
}


