package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminFilterRequest {
    // Roles filter: null = All, có thể chọn nhiều role (3 = Admin, 4 = Super Admin)
    private List<Integer> roles;
    
    // Status filter: null = All, 1 = Active (Hoạt động), 0 = Inactive (Bị Khóa)
    private Integer status;
    
    // Time of joining filter: khoảng thời gian tạo tài khoản
    private LocalDate createdAtFrom;
    private LocalDate createdAtTo;
}

