package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.AuctionRoomRequest;
import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.DTO.Request.GetRoomMembersRequest;
import com.auctionaa.backend.DTO.Request.PagingRequest;
import com.auctionaa.backend.DTO.Response.AuctionRoomLiveDTO;
import com.auctionaa.backend.DTO.Response.MemberResponse;
import com.auctionaa.backend.DTO.Response.RoomDetailDTO;
import com.auctionaa.backend.DTO.Response.RoomCompleteDetailDTO;
import com.auctionaa.backend.DTO.Response.SearchResponse;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Repository.AuctionSessionRepository;
import com.auctionaa.backend.Service.AuctionRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/auctionroom/")
public class AuctionRoomController {
    // Nam
    @Autowired
    private AuctionRoomService auctionRoomService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    AuctionSessionRepository auctionSessionRepository;

    @GetMapping("room/{id}")
    public RoomDetailDTO getRoomAndSessionByRoomId(@PathVariable String id) {
        AuctionRoom auctionRoom = auctionRoomService.getRoomById(id);
        List<AuctionSession> auctionSessionList = auctionSessionRepository.findByAuctionRoomId(id);
        return new RoomDetailDTO(auctionRoom, auctionSessionList);
    }

    /**
     * Lấy tất cả thông tin của phòng đấu giá bao gồm:
     * - Thông tin phòng (AuctionRoom)
     * - Tất cả sessions trong phòng (AuctionSession)
     * - Thông tin tác phẩm (Artwork) của mỗi session
     * GET /api/auctionroom/complete/{id}
     *
     * @param id ID của phòng đấu giá
     * @return RoomCompleteDetailDTO chứa đầy đủ thông tin
     */
    @GetMapping("complete/{id}")
    public RoomCompleteDetailDTO getRoomCompleteDetail(@PathVariable String id) {
        return auctionRoomService.getRoomCompleteDetail(id);
    }

    @PostMapping("/history")
    public List<AuctionRoom> getMyAuctionRoom(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PagingRequest req) {

        String token = authHeader.replace("Bearer ", "").trim();
        String email = jwtUtil.extractUserId(token);

        return auctionRoomService.getByOwnerEmail(email, req.getPage(), req.getSize());
    }

    @PostMapping("/allAuctionRoom")
    public List<AuctionRoomLiveDTO> getAllPublicWithLivePrices(@RequestBody PagingRequest req) {
        int page = req.getPage();
        int size = req.getSize();

        return auctionRoomService.getRoomsWithLivePrices(page, size);
    }

    /**
     * Lấy danh sách phòng đấu giá đang diễn ra (status = 1) với phân trang
     * POST /api/auctionroom/ongoing
     */
    @PostMapping("/ongoing")
    public List<AuctionRoom> getOngoingRooms(@RequestBody PagingRequest req) {
        int page = req.getPage();
        int size = req.getSize();
        return auctionRoomService.getOngoingRooms(page, size);
    }

    /**
     * Lấy danh sách phòng đấu giá sắp diễn ra (status = 2) với phân trang
     * POST /api/auctionroom/upcoming
     */
    @PostMapping("/upcoming")
    public List<AuctionRoom> getUpcomingRooms(@RequestBody PagingRequest req) {
        int page = req.getPage();
        int size = req.getSize();
        return auctionRoomService.getUpcomingRooms(page, size);
    }

    /**
     * Lấy 4 phòng đấu giá đang diễn ra (status = 1)
     * GET /api/auctionroom/top-4-ongoing
     * 
     * @return Danh sách 4 phòng đấu giá đang diễn ra
     */
    @GetMapping("/top-4-ongoing")
    public List<AuctionRoom> getTop4OngoingRooms() {
        return auctionRoomService.getTop4OngoingRooms();
    }

    /**
     * Lấy 4 phòng đấu giá sắp bắt đầu (status = 0) có giá tranh cao nhất
     * GET /api/auctionroom/top-4-upcoming-highest-price
     * 
     * Sắp xếp theo giá startingPrice cao nhất của session trong phòng
     * 
     * @return Danh sách 4 phòng đấu giá sắp bắt đầu có giá cao nhất
     */
    @GetMapping("/top-4-upcoming-highest-price")
    public List<AuctionRoom> getTop4UpcomingRoomsByHighestPrice() {
        return auctionRoomService.getTop4UpcomingRoomsByHighestPrice();
    }

    /**
     * Lấy tất cả phòng đấu giá trong database
     * GET /api/auctionroom/all
     *
     * @return Danh sách tất cả phòng đấu giá
     */
    @GetMapping("/allAuctionRoom")
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
     * Tìm kiếm auction room công khai (không yêu cầu authentication)
     * Tìm kiếm theo ID phòng hoặc tên phòng, trả về toàn bộ thông tin của auction
     * room
     * 
     * Request body (JSON):
     * - id: Tìm kiếm theo ID phòng (exact match)
     * - name: Tìm kiếm theo tên phòng (partial match, case-insensitive)
     * - type: Lọc theo thể loại
     * - dateFrom: Lọc từ ngày (format: yyyy-MM-dd)
     * - dateTo: Lọc đến ngày (format: yyyy-MM-dd)
     * 
     * Nếu gửi body rỗng {} hoặc không có điều kiện, trả về tất cả phòng đấu giá
     * 
     * @return Danh sách auction room với đầy đủ thông tin (JSON)
     */
    @PostMapping("/search-public")
    public List<AuctionRoom> searchAuctionRoomsPublic(
            @RequestBody(required = false) BaseSearchRequest request) {
        // Nếu request null hoặc không có body, tạo object mới (lấy tất cả)
        if (request == null) {
            request = new BaseSearchRequest();
        }
        return auctionRoomService.searchAuctionRoomsPublic(request);
    }

    /**
     * Lấy danh sách member của một phòng đấu giá theo ID phòng
     * POST /api/auctionroom/members
     * 
     * Request body (JSON):
     * - roomId: ID của phòng đấu giá
     * 
     * Trả về danh sách tất cả member trong phòng đấu giá, bao gồm cả admin
     * (loại trừ user hiện tại)
     *
     * @param request    Request chứa roomId
     * @param authHeader Authorization header chứa JWT token
     * @return Danh sách member với id, username, avt (không bao gồm user hiện tại)
     * @throws ResponseStatusException Nếu không tìm thấy phòng đấu giá hoặc roomId
     *                                 rỗng
     */
    @PostMapping("/members")
    public ResponseEntity<List<MemberResponse>> getRoomMembers(
            @RequestBody GetRoomMembersRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate request
            if (request == null || request.getRoomId() == null || request.getRoomId().trim().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "roomId không được để trống");
            }

            // Lấy userId hiện tại từ JWT token
            String currentUserId = jwtUtil.extractUserId(authHeader);

            List<MemberResponse> members = auctionRoomService.getRoomMembers(request.getRoomId().trim(), currentUserId);
            return ResponseEntity.ok(members);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy danh sách member: " + e.getMessage());
        }
    }

}
