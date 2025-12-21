package com.auctionaa.backend.Controller;

import com.auctionaa.backend.Config.ZegoCloudConfig;
import com.auctionaa.backend.DTO.Request.StreamStartRequest;
import AdminBackend.DTO.Response.AdminBasicResponse;
import AdminBackend.Jwt.AdminJwtUtil;
import AdminBackend.Repository.AdminRepository;
import com.auctionaa.backend.Entity.Admin;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.CloudinaryService;
import com.auctionaa.backend.Service.StreamService;
import com.auctionaa.backend.Jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final ZegoCloudConfig zegoConfig;
    private final StreamService streamService;
    private final CloudinaryService cloudinaryService;
    private final AdminJwtUtil adminJwtUtil;
    private final AdminRepository adminRepository;
    private final AuctionRoomRepository roomRepo;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    @Value("${zegocloud.server-secret}")
    private String serverSecret;

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminBasicResponse<Map<String, Object>>> creatStream(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody StreamStartRequest request
    ) throws IOException {

        // Lấy & validate token
        String token = extractBearer(authHeader);
        if (!adminJwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin token");
        }

        // Lấy adminId từ token -> tìm admin
        String adminId = adminJwtUtil.extractAdminId(token);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

        // Gắn adminId từ token vào request (override nếu có trong body)
        request.setAdminId(admin.getId());

        // Validate required fields
        if (request.getRoomName() == null || request.getRoomName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomName is required");
        }

        // Validate startedAt nếu có
        if (request.getStartedAt() != null && !request.getStartedAt().isAfter(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startedAt must be in the future");
        }

        // Validate depositAmount nếu có
        if (request.getDepositAmount() != null && request.getDepositAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "depositAmount must be >= 0");
        }

        // Validate paymentDeadlineDays nếu có
        if (request.getPaymentDeadlineDays() != null && request.getPaymentDeadlineDays() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentDeadlineDays must be > 0");
        }

        // Validate sessions
        if (request.getSessions() == null || request.getSessions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one session is required");
        }

        // Đếm số sessions sẽ được tạo
        int sessionsCount = request.getSessions().size();

        // 5️⃣ Gọi service để tạo phòng + session
        AuctionRoom room = streamService.createdStream(request, null);

        // Tạo data response
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", room.getId());
        data.put("sessionsCreated", sessionsCount);

        // Trả về kết quả theo format chuẩn
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminBasicResponse<>(1, "Auction room created successfully", data));
    }

    @PostMapping("/start/{roomId}")
    public AuctionRoom startAuctionStream(@PathVariable String roomId) {
        return streamService.startStream(roomId);
    }

    @GetMapping("/room/{roomId}")
    public AuctionRoom getRoom(@PathVariable String roomId) {
        return streamService.getRoom(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    @PostMapping("/stop/{roomId}")
    public List<Invoice> stopStream(@PathVariable String roomId) {
        return streamService.stopStreamAndGenerateInvoice(roomId);
    }

    @PostMapping("/room/{roomId}/start-next")
    public Map<String, Object> startNext(@PathVariable String roomId) {
        var s = streamService.startNextSession(roomId);
        return Map.of(
                "sessionId", s.getId(),
                "orderIndex", s.getOrderIndex(),
                "status", s.getStatus(), // 1
                "startedAt", s.getStartTime(),
                "endTime", s.getEndedAt());
    }

    @GetMapping("/token")
    public Map<String, Object> issueToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String roomId) {
        String token = extractBearer(authHeader);
        
        String userId;
        String role;
        boolean isAdmin = false;
        
        // Kiểm tra xem token có phải admin token không (dựa vào tokenType claim)
        // Chỉ kiểm tra nếu token validate được với admin secret
        boolean isAdminToken = adminJwtUtil.validateToken(token) && adminJwtUtil.isAdminToken(token);
        
        if (isAdminToken) {
            // Validate và extract admin ID
            String adminId = adminJwtUtil.extractAdminId(token);
            adminRepository.findById(adminId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
            userId = adminId;
            isAdmin = true;
        } 
        // Nếu không phải admin token, thử validate user token
        else if (jwtUtil.validateToken(token)) {
            String userExtractedId = jwtUtil.extractUserId(token);
            userRepository.findById(userExtractedId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
            userId = userExtractedId;
            isAdmin = false;
        } 
        // Nếu cả hai đều không hợp lệ
        else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        var room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // Xác định role: admin sở hữu phòng là "host", còn lại là "audience"
        if (isAdmin && userId.equals(room.getAdminId())) {
            role = "host";
        } else {
            role = "audience";
        }

        long now = System.currentTimeMillis() / 1000;
        long expireAt = now + zegoConfig.getTokenTtl();

        return Map.of(
                "appID", zegoConfig.getAppId(),
                "token", serverSecret,
                "userId", userId,
                "roomId", roomId,
                "role", role,
                "expireAt", expireAt

        );
    }

    @PostMapping("/stop-session/{sessionId}")
    public Map<String, Object> stopSession(@PathVariable String sessionId) {
        return streamService.stopAuctionSession(sessionId);
    }

    // --- helpers ---
    private String extractBearer(String header) {
        if (header == null || !header.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        return header.substring(7).trim();
    }

    @GetMapping("/room/{roomId}/sessions/current-or-next")
    public Map<String, Object> getLiveOrNext(@PathVariable String roomId) {
        return streamService.getLiveOrNextSessionWithHighestBidder(roomId);
    }
}
