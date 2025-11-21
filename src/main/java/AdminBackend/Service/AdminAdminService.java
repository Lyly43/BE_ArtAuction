package AdminBackend.Service;

import AdminBackend.DTO.Request.AddAdminRequest;
import AdminBackend.DTO.Request.UpdateAdminRequest;
import AdminBackend.DTO.Response.AdminAdminResponse;
import AdminBackend.DTO.Response.AdminBasicResponse;
import AdminBackend.DTO.Response.AdminStatisticsResponse;
import AdminBackend.DTO.Response.UpdateResponse;
import AdminBackend.Repository.AdminRepository;
import com.auctionaa.backend.Entity.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminAdminService {

    @Autowired
    private AdminRepository adminRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Admin thêm admin mới
     */
    public ResponseEntity<AdminBasicResponse<AdminAdminResponse>> addAdmin(AddAdminRequest request) {
        // Validate email unique
        if (adminRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "Email already exists", null));
        }

        // Create new admin
        Admin admin = new Admin();
        admin.setFullName(request.getFullName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setPhoneNumber(request.getPhoneNumber());
        admin.setAddress(request.getAddress());
        admin.setStatus(request.getStatus());
        admin.setRole("4"); // Default role for admin
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        admin.generateId();

        Admin savedAdmin = adminRepository.save(admin);

        AdminAdminResponse response = mapToAdminAdminResponse(savedAdmin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminBasicResponse<>(1, "Admin created successfully", response));
    }

    /**
     * Lấy tất cả admin
     */
    public ResponseEntity<List<AdminAdminResponse>> getAllAdmins() {
        List<Admin> admins = adminRepository.findAll();
        List<AdminAdminResponse> responses = admins.stream()
                .map(this::mapToAdminAdminResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Tìm kiếm admin theo ID, fullName, email, phoneNumber
     */
    public ResponseEntity<List<AdminAdminResponse>> searchAdmins(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllAdmins();
        }

        List<Admin> admins = adminRepository.searchAdmins(searchTerm.trim());
        List<AdminAdminResponse> responses = admins.stream()
                .map(this::mapToAdminAdminResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Thống kê admin
     */
    public ResponseEntity<AdminStatisticsResponse> getAdminStatistics() {
        long totalAdmins = adminRepository.count();
        long activeAdmins = adminRepository.countByStatus(1);
        long lockedAdmins = adminRepository.countByStatus(0);

        AdminStatisticsResponse statistics = new AdminStatisticsResponse();
        statistics.setTotalAdmins(totalAdmins);
        statistics.setActiveAdmins(activeAdmins);
        statistics.setLockedAdmins(lockedAdmins);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Cập nhật admin
     */
    public ResponseEntity<UpdateResponse<AdminAdminResponse>> updateAdmin(String adminId, UpdateAdminRequest request) {
        Optional<Admin> adminOpt = adminRepository.findById(adminId);

        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UpdateResponse<>(0, "Admin not found with ID: " + adminId, null));
        }

        Admin admin = adminOpt.get();

        // Kiểm tra email unique (nếu thay đổi email)
        if (request.getEmail() != null && !request.getEmail().equals(admin.getEmail())) {
            if (adminRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new UpdateResponse<>(0, "Email already exists", null));
            }
            admin.setEmail(request.getEmail());
        }

        // Cập nhật các trường khác
        if (request.getFullName() != null) {
            admin.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            admin.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            admin.setAddress(request.getAddress());
        }
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        admin.setStatus(request.getStatus());
        admin.setUpdatedAt(LocalDateTime.now());

        Admin updatedAdmin = adminRepository.save(admin);
        AdminAdminResponse response = mapToAdminAdminResponse(updatedAdmin);

        return ResponseEntity.ok(new UpdateResponse<>(1, "Admin updated successfully", response));
    }

    /**
     * Xóa admin
     */
    public ResponseEntity<AdminBasicResponse<Void>> deleteAdmin(String adminId) {
        Optional<Admin> adminOpt = adminRepository.findById(adminId);

        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Admin not found with ID: " + adminId, null));
        }

        adminRepository.delete(adminOpt.get());

        return ResponseEntity.ok(new AdminBasicResponse<>(1, "Admin deleted successfully", null));
    }

    /**
     * Helper method: Map Admin entity sang AdminAdminResponse
     */
    private AdminAdminResponse mapToAdminAdminResponse(Admin admin) {
        AdminAdminResponse response = new AdminAdminResponse();
        response.setId(admin.getId());
        response.setFullName(admin.getFullName());
        response.setEmail(admin.getEmail());
        response.setPhoneNumber(admin.getPhoneNumber());
        response.setAddress(admin.getAddress());
        response.setAvatar(admin.getAvatar());
        response.setRole(admin.getRole());
        response.setStatus(admin.getStatus());
        response.setCreatedAt(admin.getCreatedAt());
        response.setUpdatedAt(admin.getUpdatedAt());
        return response;
    }
}

