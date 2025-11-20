package AdminBackend.Controller;

import AdminBackend.DTO.Request.AdminLoginRequest;
import AdminBackend.DTO.Response.AdminAuthResponse;
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
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest request) {
        if (!StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminAuthResponse(false, "Email and password are required", null, null, null, null, null, null));
        }

        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminAuthResponse(false, "Admin not found", null, null, null, null, null, null));
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminAuthResponse(false, "Invalid password", null, null, null, null, null, null));
        }

        String role = admin.getRole() != null ? admin.getRole() : "4";
        String token = adminJwtUtil.generateAdminToken(admin.getId(), role);

        admin.setStatus("ONLINE");
        adminRepository.save(admin);

        AdminAuthResponse response = new AdminAuthResponse(
                true,
                "Admin login successfully",
                token,
                admin.getId(),
                admin.getFullName(),
                admin.getEmail(),
                role,
                admin.getAvatar()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(@RequestHeader("Authorization") String authHeader) {
        if (!adminJwtUtil.validateToken(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminAuthResponse(false, "Invalid or expired token", null, null, null, null, null, null));
        }

        String adminId = adminJwtUtil.extractAdminId(authHeader);

        Admin admin = adminRepository.findById(adminId)
                .orElse(null);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminAuthResponse(false, "Admin not found", null, null, null, null, null, null));
        }

        String role = adminJwtUtil.extractRole(authHeader);

        AdminAuthResponse response = new AdminAuthResponse(
                true,
                "Token is valid",
                authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader,
                admin.getId(),
                admin.getFullName(),
                admin.getEmail(),
                role != null ? role : admin.getRole(),
                admin.getAvatar()
        );

        return ResponseEntity.ok(response);
    }
}


