package AdminBackend.Controller;

import AdminBackend.DTO.Response.DashboardOverviewResponse;
import AdminBackend.DTO.Response.DashboardStatisticsResponse;
import AdminBackend.Service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    /**
     * GET /api/admin/dashboard/thong-ke
     * Lấy thống kê chung cho dashboard
     */
    @GetMapping("/thong-ke")
    public ResponseEntity<DashboardStatisticsResponse> getDashboardStatistics() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStatistics());
    }

    /**
     * GET /api/admin/dashboard/top-auction-rooms
     * Lấy top 10 AuctionRoom mới nhất với sessions
     */
    @GetMapping("/top-auction-rooms")
    public ResponseEntity<List<DashboardOverviewResponse.AuctionRoomWithSessions>> getTop10AuctionRooms() {
        return ResponseEntity.ok(adminDashboardService.getTop10AuctionRooms());
    }

    /**
     * GET /api/admin/dashboard/top-new-users
     * Lấy top 10 User mới đăng ký nhất
     */
    @GetMapping("/top-new-users")
    public ResponseEntity<List<DashboardOverviewResponse.NewUserInfo>> getTop10NewUsers() {
        return ResponseEntity.ok(adminDashboardService.getTop10NewUsers());
    }

    /**
     * GET /api/admin/dashboard/top-sessions
     * Lấy top 10 Session có giá cao nhất
     */
    @GetMapping("/top-sessions")
    public ResponseEntity<List<DashboardOverviewResponse.TopSessionInfo>> getTop10Sessions() {
        return ResponseEntity.ok(adminDashboardService.getTop10SessionsByPrice());
    }
}

