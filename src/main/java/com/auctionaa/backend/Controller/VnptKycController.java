package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Response.KycSubmitResponse;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Slf4j
public class VnptKycController {

    private final KycService kycService;
    private final JwtUtil jwtUtil;

    @PostMapping(path = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KycSubmitResponse> submit(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("roomId") String roomId,
            @RequestPart(value = "cccdFront", required = false) MultipartFile cccdFront,
            @RequestPart(value = "cccdBack", required = false) MultipartFile cccdBack,
            @RequestPart(value = "selfie", required = false) MultipartFile selfie) {

        // Lấy userId từ JWT token
        String userId = jwtUtil.extractUserId(authHeader);
        log.info("VNPT KYC submit userId={}, roomId={}", userId, roomId);

        // Service sẽ tự động check:
        // - Nếu user đã là buyer (role = 1, kycStatus = 1) → không cần file ảnh, chỉ
        // cần roomId
        // - Nếu chưa là buyer → bắt buộc phải có file ảnh để verify KYC
        return ResponseEntity.ok(kycService.submitAndVerifyWithFiles(userId, roomId, cccdFront, cccdBack, selfie));
    }
}
