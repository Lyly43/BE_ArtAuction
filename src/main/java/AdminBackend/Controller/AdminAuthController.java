package AdminBackend.Controller;

import AdminBackend.DTO.Request.AdminLoginRequest;
import AdminBackend.DTO.Response.AdminCheckTokenResponse;
import AdminBackend.DTO.Response.AdminLoginResponse;
import AdminBackend.Jwt.AdminJwtUtil;
import AdminBackend.Repository.AdminRepository;
import com.auctionaa.backend.Entity.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminAuthController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdminJwtUtil adminJwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        if (!StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminLoginResponse(0, "Email and password are required", null));
        }

        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminLoginResponse(0, "Admin not found", null));
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminLoginResponse(0, "Invalid password", null));
        }

        // Kiểm tra trạng thái admin: 0 = Bị Khóa, 1 = Hoạt động
        if (admin.getStatus() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AdminLoginResponse(0, "Admin account is locked", null));
        }

        String role = admin.getRole() != null ? admin.getRole() : "4";
        String token = adminJwtUtil.generateAdminToken(admin.getId(), role);

        // Status đã là 1 (Hoạt động) trong database, không cần set lại

        AdminLoginResponse response = new AdminLoginResponse(1, "Admin login successfully", token);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AdminCheckTokenResponse> getCurrentAdmin(@RequestHeader("Authorization") String authHeader) {
        if (!adminJwtUtil.validateToken(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminCheckTokenResponse(0, "Invalid or expired token", null, null, null));
        }

        String adminId = adminJwtUtil.extractAdminId(authHeader);

        Admin admin = adminRepository.findById(adminId)
                .orElse(null);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminCheckTokenResponse(0, "Admin not found", null, null, null));
        }

        String role = adminJwtUtil.extractRole(authHeader);

        AdminCheckTokenResponse response = new AdminCheckTokenResponse(
                1,
                "Token is valid",
                admin.getFullName(),
                admin.getEmail(),
                admin.getAvatar()
        );

        return ResponseEntity.ok(response);
    }
}


