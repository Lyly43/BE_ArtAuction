package AdminBackend.DTO.Response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminUserResponse {
    private String id;
    private String fullname; // username
    private String email;
    private String phonenumber;
    private Integer gender; // 0 = male, 1 = female, 2 = other
    private LocalDate dateOfBirth;
    private String address;
    private String cccd;
    private int role; // 1 = normal user, 2 = buyer, 3 = seller
    private int status; // 1 = active, 0 = offline, 2 = blocked
    private BigDecimal balance; // from Wallet entity
    private String avt; // Avatar URL
    private LocalDateTime createdAt;
}

