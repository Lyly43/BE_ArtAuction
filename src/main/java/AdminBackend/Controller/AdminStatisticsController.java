package AdminBackend.Controller;

import AdminBackend.DTO.Request.DateRangeRequest;
import AdminBackend.DTO.Response.ChartDataResponse;
import AdminBackend.Service.AdminStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/statistics")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminStatisticsController {

    @Autowired
    private AdminStatisticsService adminStatisticsService;

    /**
     * POST /api/admin/statistics/users-registration
     * Thống kê số người dùng đăng ký trong khoảng thời gian
     */
    @PostMapping("/users-registration")
    public ResponseEntity<ChartDataResponse> getUsersRegistrationStats(
            @RequestBody DateRangeRequest request) {
        return ResponseEntity.ok(adminStatisticsService.getUsersRegistrationStats(request));
    }

    /**
     * POST /api/admin/statistics/auction-rooms
     * Thống kê số phòng đấu giá được lập trong khoảng thời gian
     */
    @PostMapping("/auction-rooms")
    public ResponseEntity<ChartDataResponse> getAuctionRoomsStats(
            @RequestBody DateRangeRequest request) {
        return ResponseEntity.ok(adminStatisticsService.getAuctionRoomsStats(request));
    }

    /**
     * POST /api/admin/statistics/revenue
     * Thống kê doanh thu (invoice) trong khoảng thời gian
     */
    @PostMapping("/revenue")
    public ResponseEntity<ChartDataResponse> getRevenueStats(
            @RequestBody DateRangeRequest request) {
        return ResponseEntity.ok(adminStatisticsService.getRevenueStats(request));
    }

    /**
     * POST /api/admin/statistics/reports
     * Thống kê số report được báo cáo trong khoảng thời gian
     */
    @PostMapping("/reports")
    public ResponseEntity<ChartDataResponse> getReportsStats(
            @RequestBody DateRangeRequest request) {
        return ResponseEntity.ok(adminStatisticsService.getReportsStats(request));
    }

    /**
     * POST /api/admin/statistics/artworks
     * Thống kê số tác phẩm được thêm vào trong khoảng thời gian
     */
    @PostMapping("/artworks")
    public ResponseEntity<ChartDataResponse> getArtworksStats(
            @RequestBody DateRangeRequest request) {
        return ResponseEntity.ok(adminStatisticsService.getArtworksStats(request));
    }

    /**
     * POST /api/admin/statistics/bids
     * Thống kê số đấu giá (bids) trong khoảng thời gian
     */
    @PostMapping("/bids")
    public ResponseEntity<ChartDataResponse> getBidsStats(
            @RequestBody DateRangeRequest request) {
        return ResponseEntity.ok(adminStatisticsService.getBidsStats(request));
    }
}

