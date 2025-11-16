package com.auctionaa.backend.Controller;

import com.auctionaa.backend.Config.ZegoCloudConfig;
import com.auctionaa.backend.DTO.Request.AuctionSessionCreateRequest;
import com.auctionaa.backend.DTO.Request.StreamStartRequest;
import com.auctionaa.backend.DTO.Response.StreamStartResponse;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.CloudinaryService;
import com.auctionaa.backend.Service.StreamService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final ZegoCloudConfig zegoConfig;
    private final StreamService streamService;
    private final CloudinaryService cloudinaryService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuctionRoomRepository roomRepo;
    @Value("${zegocloud.server-secret}")
    private String serverSecret;
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StreamStartResponse creatStream(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String roomName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String sessionsJson // FE gửi JSON sessions ở đây
    ) throws IOException {

        // Lấy & validate token
        String token = extractBearer(authHeader);
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }


        // Lấy userId từ token -> tìm user
        String userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Parse danh sách sessions từ FE (nếu có)
        List<AuctionSessionCreateRequest> sessions = new ArrayList<>();
        if (sessionsJson != null && !sessionsJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                sessions = mapper.readValue(sessionsJson, new TypeReference<List<AuctionSessionCreateRequest>>() {});
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sessions format: " + e.getMessage());
            }
        }

        // Build request cho service
        StreamStartRequest rq = new StreamStartRequest();
        rq.setAdminId(user.getId()); // gắn adminId
        rq.setRoomName(roomName);
        rq.setDescription(description);
        rq.setType(type == null ? "public" : type);
        rq.setSessions(sessions); // thêm danh sách session

        // 5️⃣ Gọi service để tạo phòng + session
        AuctionRoom room = streamService.createdStream(rq, file);


        // Trả về kết quả
        return StreamStartResponse.builder()
                .roomId(room.getId())
                .wsUrl("ws://localhost:8081/ws/stream/" + room.getId())
                .status(room.getStatus())
                .build();
    }

    @PostMapping("/start/{roomId}")
    public AuctionRoom startAuctionStream(@PathVariable String roomId) {
        return streamService.startStream(roomId);
    }

    @GetMapping("/room/{roomId}")
    public AuctionRoom getRoom(@PathVariable String roomId){
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
                "sessionId",  s.getId(),
                "orderIndex", s.getOrderIndex(),
                "status",     s.getStatus(),  // 1
                "startedAt",  s.getStartTime()
        );
    }



    @GetMapping("/token")
    public Map<String, Object> issueToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String roomId
    ) {
        String userId = jwtUtil.extractUserId(authHeader);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        var room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        String role = userId.equals(room.getAdminId()) ? "host" : "audience";

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
    public AuctionSession getLiveOrNext(@PathVariable String roomId) {
        return streamService.getLiveOrNextSessionInRoom(roomId);
    }
}
