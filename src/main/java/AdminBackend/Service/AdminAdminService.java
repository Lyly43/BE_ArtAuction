package AdminBackend.Service;

import AdminBackend.DTO.Request.AddAdminRequest;
import AdminBackend.DTO.Request.AdminFilterRequest;
import AdminBackend.DTO.Request.UpdateAdminRequest;
import AdminBackend.DTO.Response.AdminAdminResponse;
import AdminBackend.DTO.Response.AdminBasicResponse;
import AdminBackend.DTO.Response.AdminStatisticsResponse;
import AdminBackend.DTO.Response.UpdateResponse;
import AdminBackend.Repository.AdminRepository;
import com.auctionaa.backend.Entity.Admin;
import com.auctionaa.backend.Service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminAdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Lấy admin hiện tại từ SecurityContext (dựa trên adminId trong JWT)
     */
    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated admin");
        }
        String adminId = (String) authentication.getPrincipal();
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found for id from token: " + adminId));
    }

    /**
     * Kiểm tra có phải super admin (role = 4) hay không
     */
    private boolean isSuperAdmin(Admin admin) {
        return admin != null && admin.getRole() != null && admin.getRole() == 4;
    }

    /**
     * Admin thêm admin mới (JSON thuần, avatar là URL đã upload trước đó)
     */
    public ResponseEntity<AdminBasicResponse<AdminAdminResponse>> addAdmin(AddAdminRequest request) {
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AdminBasicResponse<>(0, "Only super admin (role = 4) can create admins", null));
        }
        String fullName = request.getFullName();
        String email = request.getEmail();
        String password = request.getPassword();
        String phoneNumber = request.getPhoneNumber();
        String address = request.getAddress();
        Integer status = request.getStatus() != null ? request.getStatus() : 1;
        Integer role = request.getRole() != null ? request.getRole() : 3;
        String avatarUrl = request.getAvatar();

        // Validate email unique
        if (adminRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "Email already exists", null));
        }

        // Create new admin
        Admin admin = new Admin();
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setPhoneNumber(phoneNumber);
        admin.setAddress(address);
        admin.setStatus(status);
        admin.setRole(role);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        admin.generateId();

        // Set avatar URL nếu đã upload trước đó
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            admin.setAvatar(avatarUrl);
        }

        Admin savedAdmin = adminRepository.save(admin);

        AdminAdminResponse response = mapToAdminAdminResponse(savedAdmin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminBasicResponse<>(1, "Admin created successfully", response));
    }

    /**
     * Upload avatar admin từ thiết bị và trả về URL
     * Frontend sẽ gọi endpoint này trước, lấy URL rồi set vào field avatar của AddAdminRequest
     */
    public ResponseEntity<AdminBasicResponse<java.util.Map<String, String>>> uploadAdminAvatar(MultipartFile avatarFile) {
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AdminBasicResponse<>(0, "Only super admin (role = 4) can upload admin avatar", null));
        }
        if (avatarFile == null || avatarFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "File avatar không được để trống", null));
        }

        try {
            // Dùng ID tạm thời để lưu avatar, không gắn với admin cụ thể
            String tempAdminId = "temp-" + System.currentTimeMillis();
            CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadAdminAvatar(tempAdminId, avatarFile);

            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("avatarUrl", uploadResult.getUrl());
            data.put("publicId", uploadResult.getPublicId());

            return ResponseEntity.ok(new AdminBasicResponse<>(1, "Upload avatar thành công", data));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminBasicResponse<>(0, "Failed to upload avatar: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy tất cả admin
     */
    public ResponseEntity<List<AdminAdminResponse>> getAllAdmins() {
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
     * Lọc admin theo các tiêu chí: roles, status, createdAt
     */
    public ResponseEntity<List<AdminAdminResponse>> filterAdmins(AdminFilterRequest request) {
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Nếu request null, trả về tất cả admins
        if (request == null) {
            return getAllAdmins();
        }
        
        List<Admin> allAdmins = adminRepository.findAll();
        
        List<Admin> filteredAdmins = allAdmins.stream()
                .filter(admin -> {
                    // Filter by roles (null hoặc empty = bỏ qua filter)
                    if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                        if (admin.getRole() == null || !request.getRoles().contains(admin.getRole())) {
                            return false;
                        }
                    }
                    
                    // Filter by status (null = bỏ qua filter)
                    if (request.getStatus() != null && admin.getStatus() != null) {
                        if (!admin.getStatus().equals(request.getStatus())) {
                            return false;
                        }
                    }
                    
                    // Filter by createdAt range (null = bỏ qua filter)
                    // createdAtFrom: admin.getCreatedAt() >= createdAtFrom
                    if (request.getCreatedAtFrom() != null) {
                        if (admin.getCreatedAt() == null) {
                            return false;
                        }
                        LocalDate adminCreatedDate = admin.getCreatedAt().toLocalDate();
                        if (adminCreatedDate.isBefore(request.getCreatedAtFrom())) {
                            return false;
                        }
                    }
                    // createdAtTo: admin.getCreatedAt() <= createdAtTo
                    if (request.getCreatedAtTo() != null) {
                        if (admin.getCreatedAt() == null) {
                            return false;
                        }
                        LocalDate adminCreatedDate = admin.getCreatedAt().toLocalDate();
                        if (adminCreatedDate.isAfter(request.getCreatedAtTo())) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        List<AdminAdminResponse> responses = filteredAdmins.stream()
                .map(this::mapToAdminAdminResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Thống kê admin
     */
    public ResponseEntity<AdminStatisticsResponse> getAdminStatistics() {
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UpdateResponse<>(0, "Only super admin (role = 4) can update admins", null));
        }
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
        if (request.getAvatar() != null) {
            admin.setAvatar(request.getAvatar());
        }
        if (request.getRole() != null) {
            admin.setRole(request.getRole());
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
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AdminBasicResponse<>(0, "Only super admin (role = 4) can delete admins", null));
        }
        Optional<Admin> adminOpt = adminRepository.findById(adminId);

        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Admin not found with ID: " + adminId, null));
        }

        adminRepository.delete(adminOpt.get());

        return ResponseEntity.ok(new AdminBasicResponse<>(1, "Admin deleted successfully", null));
    }

    /**
     * Lấy thông tin chi tiết 1 admin theo ID
     * Chỉ super admin (role = 4) mới được phép gọi
     */
    public ResponseEntity<AdminBasicResponse<AdminAdminResponse>> getAdminById(String adminId) {
        Admin currentAdmin = getCurrentAdmin();
        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AdminBasicResponse<>(0, "Only super admin (role = 4) can view admin detail", null));
        }

        Optional<Admin> adminOpt = adminRepository.findById(adminId);
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Admin not found with ID: " + adminId, null));
        }

        AdminAdminResponse response = mapToAdminAdminResponse(adminOpt.get());
        return ResponseEntity.ok(new AdminBasicResponse<>(1, "Success", response));
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
        response.setRole(admin.getRole() != null ? admin.getRole() : 3); // Default role 3 (admin)
        response.setStatus(admin.getStatus() != null ? admin.getStatus() : 1); // Default status 1 (Hoạt động)
        response.setCreatedAt(admin.getCreatedAt());
        response.setUpdatedAt(admin.getUpdatedAt());
        return response;
    }
}


