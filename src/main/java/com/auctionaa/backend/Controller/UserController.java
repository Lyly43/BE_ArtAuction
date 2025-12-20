package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.UserRequest;
import com.auctionaa.backend.DTO.Response.CheckUserInRoomResponse;
import com.auctionaa.backend.DTO.Response.KycVerifyResponse;
import com.auctionaa.backend.DTO.Response.UserAVTResponse;
import com.auctionaa.backend.DTO.Response.UserResponse;
import com.auctionaa.backend.DTO.Response.UserRoleResponse;
import com.auctionaa.backend.DTO.Response.UserTradeStatsResponse;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Service.KycService;
import com.auctionaa.backend.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user") // <— bỏ dấu "/" cuối
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private KycService kycService;
    @Autowired
    private AuctionRoomRepository auctionRoomRepository;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/info")
    public ResponseEntity<?> getInfoUser(@RequestHeader("Authorization") String authHeader) {
        // giữ nguyên (nhưng nên sanitize token như dưới cho chắc)
        return userService.getUserInfo(authHeader);
    }

    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> updateProfileJson(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UserRequest dto) {

        String userId = jwtUtil.extractUserId(authHeader); // <— dùng userId từ token (đã sanitize bên trong)
        return ResponseEntity.ok(userService.updateUserById(userId, dto)); // <— gọi theo id
    }

    @PutMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserAVTResponse> updateAvatar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("avatarFile") MultipartFile avatarFile) {

        String userId = jwtUtil.extractUserId(authHeader); // <— dùng userId
        return ResponseEntity.ok(userService.updateAvatarById(userId, avatarFile)); // <— gọi theo id
    }

    @PostMapping(value = "/kyc/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KycVerifyResponse> verifyKyc(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("cccdFront") MultipartFile cccdFront,
            @RequestParam("cccdBack") MultipartFile cccdBack,
            @RequestParam("selfie") MultipartFile selfie) {

        String userId = jwtUtil.extractUserId(authHeader);
        return ResponseEntity.ok(kycService.verifyKycForProfile(userId, cccdFront, cccdBack, selfie));
    }

    @GetMapping("/trade-stats")
    public ResponseEntity<UserTradeStatsResponse> getTradeStats(
            @RequestHeader("Authorization") String authHeader) {
        String userId = jwtUtil.extractUserId(authHeader);
        return ResponseEntity.ok(userService.getTradingStats(userId));
    }

    /**
     * Kiểm tra user có nằm trong member list của auction room hay không
     * GET /api/user/check-in-room/{roomId}
     * 
     * @param roomId ID của auction room cần kiểm tra
     * @param authHeader Authorization header chứa JWT token
     * @return CheckUserInRoomResponse với status 1 (có) hoặc 0 (không có) và message tương ứng
     */
    @GetMapping("/check-in-room/{roomId}")
    public ResponseEntity<CheckUserInRoomResponse> checkUserInRoom(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String authHeader) {
        
        // Lấy userId từ JWT token
        String userId = jwtUtil.extractUserId(authHeader);
        
        // Tìm auction room theo ID
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElse(null);
        
        // Nếu không tìm thấy phòng
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CheckUserInRoomResponse(0, "Không tìm thấy phòng đấu giá với ID: " + roomId));
        }
        
        // Kiểm tra user có trong memberIds không
        boolean isMember = room.getMemberIds() != null && room.getMemberIds().contains(userId);
        
        if (isMember) {
            return ResponseEntity.ok(new CheckUserInRoomResponse(
                    1, 
                    "Có người dùng trong phòng " + roomId
            ));
        } else {
            return ResponseEntity.ok(new CheckUserInRoomResponse(
                    0, 
                    "Không có user trong phòng " + roomId
            ));
        }
    }

    /**
     * Lấy role hiện tại của user
     * GET /api/user/role
     * 
     * @param authHeader Authorization header chứa JWT token
     * @return UserRoleResponse chứa role và roleName của user hiện tại
     */
    @GetMapping("/role")
    public ResponseEntity<UserRoleResponse> getCurrentUserRole(
            @RequestHeader("Authorization") String authHeader) {
        
        // Lấy userId từ JWT token
        String userId = jwtUtil.extractUserId(authHeader);
        
        // Tìm user trong database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "User not found"
                ));
        
        // Map role sang roleName
        String roleName = switch (user.getRole()) {
            case 0 -> "User";
            case 1 -> "Buyer";
            case 2 -> "Seller";
            default -> "Unknown";
        };
        
        return ResponseEntity.ok(new UserRoleResponse(user.getRole(), roleName));
    }
}
