package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.AuctionRoomRequest;
import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.DTO.Response.AuctionRoomLiveDTO;
import com.auctionaa.backend.DTO.Response.MemberResponse;
import com.auctionaa.backend.DTO.Response.SearchResponse;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.AuctionRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/auctionroom/")
public class AuctionRoomController {
    @Autowired
    private AuctionRoomService auctionRoomService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/history")
    public List<AuctionRoom> getMyAuctionRoom(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();
        String email = jwtUtil.extractUserId(token);
        return auctionRoomService.getByOwnerEmail(email);
    }

    @GetMapping("/allAuctionRoom")
    public List<AuctionRoomLiveDTO> getAllPublicWithLivePrices() {
        return auctionRoomService.getRoomsWithLivePrices();
    }

    /**
     * Lấy tất cả phòng đấu giá trong database
     * GET /api/auctionroom/all
     *
     * @return Danh sách tất cả phòng đấu giá
     */
    @GetMapping("/all")
    public List<AuctionRoom> getAllRooms() {
        return auctionRoomService.getAllAuctionRoom();
    }

    // Endpoint tạo auction room mới
    @PostMapping("/create")
    public AuctionRoom createAuctionRoom(@RequestBody AuctionRoomRequest req,
                                         @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();
        String email = jwtUtil.extractUserId(token);
        return auctionRoomService.createAuctionRoom(req, email);
    }

    @GetMapping("/featuredAuctionRoom")
    public List<AuctionRoomLiveDTO> getSix() {
        return auctionRoomService.getTop6HotAuctionRooms();
    }

    /**
     * Tìm kiếm và lọc auction room của user hiện tại
     * Request body (JSON):
     * - id: Tìm kiếm theo ID (exact match)
     * - name: Tìm kiếm theo tên phòng (partial match, case-insensitive)
     * - type: Lọc theo thể loại
     * - dateFrom: Lọc từ ngày (format: yyyy-MM-dd)
     * - dateTo: Lọc đến ngày (format: yyyy-MM-dd)
     * Có thể gửi body rỗng {} để lấy tất cả phòng mà user tham gia (là admin hoặc
     * member)
     */
    @PostMapping("/search")
    public SearchResponse<AuctionRoom> searchAndFilter(
            @RequestBody(required = false) BaseSearchRequest request,
            @RequestHeader("Authorization") String authHeader) {
        // Nếu request null hoặc không có body, tạo object mới (lấy tất cả)
        if (request == null) {
            request = new BaseSearchRequest();
        }
        // Lấy userId từ JWT token
        String userId = jwtUtil.extractUserId(authHeader);
        List<AuctionRoom> results = auctionRoomService.searchAndFilter(request, userId);
        return SearchResponse.success(results);
    }

    /**
     * Lấy danh sách member của một phòng đấu giá
     * GET /api/auctionroom/{roomId}/members
     *
     * @param roomId ID của phòng đấu giá
     * @return Danh sách member với id, username, avt
     */
    @GetMapping("/{roomId}/members")
    public List<MemberResponse> getRoomMembers(@PathVariable String roomId) {
        return auctionRoomService.getRoomMembers(roomId);
    }

}
