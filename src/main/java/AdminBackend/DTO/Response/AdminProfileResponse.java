package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO cho API lấy thông tin đầy đủ của admin hiện tại
 * Trả về tất cả các trường của admin (trừ password)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileResponse {
    private int status; // 1 = success, 0 = error
    private String message;
    private AdminData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminData {
        private String id;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String address;
        private String avatar;
        private Integer role; // 3: Admin, 4: Super Admin
        private Integer status; // 0 = Bị Khóa, 1 = Hoạt động
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}

