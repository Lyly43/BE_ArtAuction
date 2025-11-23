package AdminBackend.Controller;

import AdminBackend.DTO.Request.AddAuctionRoomRequest;
import AdminBackend.DTO.Request.CreateAuctionRoomCompleteRequest;
import AdminBackend.DTO.Request.UpdateAuctionRoomRequest;
import AdminBackend.DTO.Response.AdminAuctionRoomResponse;
import AdminBackend.DTO.Response.AuctionRoomStatisticsResponse;
import AdminBackend.Service.AdminAuctionRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auction-rooms")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminAuctionRoomController {

    @Autowired
    private AdminAuctionRoomService adminAuctionRoomService;

    @PostMapping("/them-phong")
    public ResponseEntity<?> addAuctionRoom(@RequestBody AddAuctionRoomRequest request) {
        return adminAuctionRoomService.addAuctionRoom(request);
    }

    @GetMapping("/lay-du-lieu")
    public ResponseEntity<List<AdminAuctionRoomResponse>> getAllAuctionRooms() {
        return adminAuctionRoomService.getAllAuctionRooms();
    }

    @GetMapping("/tim-kiem")
    public ResponseEntity<List<AdminAuctionRoomResponse>> searchAuctionRooms(
            @RequestParam(value = "q", required = false) String searchTerm) {
        return adminAuctionRoomService.searchAuctionRooms(searchTerm);
    }

    @PutMapping("/cap-nhat/{roomId}")
    public ResponseEntity<?> updateAuctionRoom(
            @PathVariable String roomId,
            @RequestBody UpdateAuctionRoomRequest request) {
        return adminAuctionRoomService.updateAuctionRoom(roomId, request);
    }

    @DeleteMapping("/xoa/{roomId}")
    public ResponseEntity<?> deleteAuctionRoom(@PathVariable String roomId) {
        return adminAuctionRoomService.deleteAuctionRoom(roomId);
    }

    @GetMapping("/thong-ke")
    public ResponseEntity<AuctionRoomStatisticsResponse> getStatistics() {
        return adminAuctionRoomService.getAuctionRoomStatistics();
    }

    /**
     * GET /api/admin/auction-rooms/thong-ke-monthly
     * Thống kê so sánh tháng này vs tháng trước cho auction rooms
     */
    @GetMapping("/thong-ke-monthly")
    public ResponseEntity<?> getAuctionRoomMonthlyComparison() {
        return adminAuctionRoomService.getAuctionRoomMonthlyComparison();
    }

    /**
     * POST /api/admin/auction-rooms/tao-phong-hoan-chinh
     * Tạo phòng đấu giá hoàn chỉnh với tất cả thông tin:
     * - Thông tin phòng (roomName, description, startedAt, adminId, ...)
     * - Danh sách tác phẩm với startingPrice và bidStep
     * - Cấu hình tài chính (depositAmount, paymentDeadlineDays)
     */
    @PostMapping("/tao-phong-hoan-chinh")
    public ResponseEntity<?> createAuctionRoomComplete(@RequestBody CreateAuctionRoomCompleteRequest request) {
        return adminAuctionRoomService.createAuctionRoomComplete(request);
    }
}

