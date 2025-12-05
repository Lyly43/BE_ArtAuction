package AdminBackend.Controller;

import AdminBackend.DTO.Request.AddAuctionRoomRequest;
import AdminBackend.DTO.Request.AuctionRoomFilterRequest;
import AdminBackend.DTO.Request.CreateAuctionRoomCompleteRequest;
import AdminBackend.DTO.Request.UpdateAuctionRoomRequest;
import AdminBackend.DTO.Response.AdminAuctionRoomResponse;
import AdminBackend.DTO.Response.AuctionRoomStatisticsResponse;
import AdminBackend.Service.AdminAuctionRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
     * POST /api/admin/auction-rooms/tao-phong-hoan-chinh
     * Tạo phòng đấu giá hoàn chỉnh với tất cả thông tin
     * - Thông tin phòng (roomName, description, startedAt, adminId, ...)
     * - Danh sách tác phẩm với startingPrice và bidStep
     * - Cấu hình tài chính (depositAmount, paymentDeadlineDays)
     * - imageAuctionRoom: URL string (đã được upload từ endpoint upload-ảnh)
     */
    @PostMapping(value = "/tao-phong-hoan-chinh", consumes = "application/json")
    public ResponseEntity<?> createAuctionRoomComplete(@RequestBody CreateAuctionRoomCompleteRequest request) {
        return adminAuctionRoomService.createAuctionRoomComplete(request);
    }


    /**
     * POST /api/admin/auction-rooms/loc-phong-dau-gia
     * Lọc phòng đấu giá theo các tiêu chí: status, startTime, endTime, participants
     * Yêu cầu: Content-Type: application/json
     */
    @PostMapping(value = "/loc-phong-dau-gia", consumes = "application/json")
    public ResponseEntity<List<AdminAuctionRoomResponse>> filterAuctionRooms(@RequestBody AuctionRoomFilterRequest request) {
        return adminAuctionRoomService.filterAuctionRooms(request);
    }

    /**
     * GET /api/admin/auction-rooms/artworks
     * Lấy danh sách tất cả artworks có thể thêm vào phòng đấu giá
     * Chỉ trả về artworks đã được duyệt (status = 1) và chưa có trong session đang diễn ra
     */
    @GetMapping("/artworks")
    public ResponseEntity<List<AdminBackend.DTO.Response.ArtworkForSelectionResponse>> getAvailableArtworks() {
        return adminAuctionRoomService.getAvailableArtworks();
    }

    /**
     * GET /api/admin/auction-rooms/{roomId}
     * Lấy chi tiết phòng đấu giá theo ID
     * Lưu ý: Endpoint này phải đặt sau các endpoint cụ thể khác để tránh conflict
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getAuctionRoomDetail(@PathVariable String roomId) {
        return adminAuctionRoomService.getAuctionRoomDetail(roomId);
    }
}

