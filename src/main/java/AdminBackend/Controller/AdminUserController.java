package AdminBackend.Controller;

import AdminBackend.DTO.Request.AddUserRequest;
import AdminBackend.DTO.Request.UpdateUserRequest;
import AdminBackend.DTO.Response.AdminUserResponse;
import AdminBackend.DTO.Response.UserStatisticsResponse;
import AdminBackend.Service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * POST /api/admin/them-user
     * Admin thêm người dùng mới
     */
    @PostMapping("/them-user")
    public ResponseEntity<?> addUser(@RequestBody AddUserRequest request) {
        return adminUserService.addUser(request);
    }

    /**
     * GET /api/admin/lay-du-lieu-user
     * Lấy tất cả người dùng với đầy đủ thông tin
     */
    @GetMapping("/lay-du-lieu-user")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return adminUserService.getAllUsers();
    }

    /**
     * GET /api/admin/tim-kiem-user?q={searchTerm}
     * Tìm kiếm người dùng theo ID, username, phonenumber, cccd
     */
    @GetMapping("/tim-kiem-user")
    public ResponseEntity<List<AdminUserResponse>> searchUsers(
            @RequestParam(value = "q", required = false) String searchTerm) {
        return adminUserService.searchUsers(searchTerm);
    }

    /**
     * GET /api/admin/thong-ke-user
     * Lấy thống kê người dùng (tổng người dùng, tổng người bán, tổng người bị khóa)
     */
    @GetMapping("/thong-ke-user")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        return adminUserService.getUserStatistics();
    }

    /**
     * PUT /api/admin/cap-nhat-user/{userId}
     * Admin cập nhật thông tin người dùng
     */
    @PutMapping("/cap-nhat-user/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable String userId,
            @RequestBody UpdateUserRequest request) {
        return adminUserService.updateUser(userId, request);
    }

    /**
     * DELETE /api/admin/xoa-user/{userId}
     * Admin xóa người dùng
     */
    @DeleteMapping("/xoa-user/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        return adminUserService.deleteUser(userId);
    }
}

