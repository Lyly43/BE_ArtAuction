package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.UserRequest;
import com.auctionaa.backend.DTO.Response.KycVerifyResponse;
import com.auctionaa.backend.DTO.Response.UserAVTResponse;
import com.auctionaa.backend.DTO.Response.UserResponse;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.KycService;
import com.auctionaa.backend.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
