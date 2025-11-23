package AdminBackend.DTO.Request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateAdminRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private int status; // 0 = Bị Khóa, 1 = Hoạt động
    private String password; // Optional - chỉ update nếu có giá trị
}


