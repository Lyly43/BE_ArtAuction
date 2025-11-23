package AdminBackend.Controller;

import AdminBackend.DTO.Request.AddAdminRequest;
import AdminBackend.DTO.Request.UpdateAdminRequest;
import AdminBackend.DTO.Response.AdminAdminResponse;
import AdminBackend.DTO.Response.AdminStatisticsResponse;
import AdminBackend.Service.AdminAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/admins")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminAdminController {

    @Autowired
    private AdminAdminService adminAdminService;

    /**
     * POST /api/admin/admins/them-admin
     * Admin thêm admin mới
     */
    @PostMapping("/them-admin")
    public ResponseEntity<?> addAdmin(@RequestBody AddAdminRequest request) {
        return adminAdminService.addAdmin(request);
    }

    /**
     * GET /api/admin/admins/lay-du-lieu
     * Lấy tất cả admin với đầy đủ thông tin
     */
    @GetMapping("/lay-du-lieu")
    public ResponseEntity<List<AdminAdminResponse>> getAllAdmins() {
        return adminAdminService.getAllAdmins();
    }

    /**
     * GET /api/admin/admins/tim-kiem?q={searchTerm}
     * Tìm kiếm admin theo ID, fullName, email, phoneNumber
     */
    @GetMapping("/tim-kiem")
    public ResponseEntity<List<AdminAdminResponse>> searchAdmins(
            @RequestParam(value = "q", required = false) String searchTerm) {
        return adminAdminService.searchAdmins(searchTerm);
    }

    /**
     * GET /api/admin/admins/thong-ke
     * Thống kê admin: tổng admin, admin hoạt động, admin bị khóa
     */
    @GetMapping("/thong-ke")
    public ResponseEntity<AdminStatisticsResponse> getAdminStatistics() {
        return adminAdminService.getAdminStatistics();
    }

    /**
     * PUT /api/admin/admins/cap-nhat/{adminId}
     * Cập nhật thông tin admin
     */
    @PutMapping("/cap-nhat/{adminId}")
    public ResponseEntity<?> updateAdmin(
            @PathVariable String adminId,
            @RequestBody UpdateAdminRequest request) {
        return adminAdminService.updateAdmin(adminId, request);
    }

    /**
     * DELETE /api/admin/admins/xoa/{adminId}
     * Xóa admin
     */
    @DeleteMapping("/xoa/{adminId}")
    public ResponseEntity<?> deleteAdmin(@PathVariable String adminId) {
        return adminAdminService.deleteAdmin(adminId);
    }
}


