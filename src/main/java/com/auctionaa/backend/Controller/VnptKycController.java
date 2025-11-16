package com.auctionaa.backend.Controller;


import com.auctionaa.backend.DTO.Request.KycSubmitRequest;
import com.auctionaa.backend.DTO.Response.KycSubmitResponse;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Slf4j
public class VnptKycController {

    private final KycService kycService;
    private final JwtUtil jwtUtil;

    @PostMapping(
            path = "/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<KycSubmitResponse> submit(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("roomId") String roomId,
            @RequestPart("cccdFront") MultipartFile cccdFront,
            @RequestPart("cccdBack") MultipartFile cccdBack,
            @RequestPart("selfie") MultipartFile selfie
    ) throws IOException {

        // Lấy userId từ JWT token
        String userId = jwtUtil.extractUserId(authHeader);
        log.info("VNPT KYC submit userId={}, roomId={}", userId, roomId);

        String frontBase64 = Base64.getEncoder().encodeToString(cccdFront.getBytes());
        String backBase64  = Base64.getEncoder().encodeToString(cccdBack.getBytes());
        String selfieBase64 = Base64.getEncoder().encodeToString(selfie.getBytes());

        KycSubmitRequest req = new KycSubmitRequest();
        req.setUserId(userId);
        req.setRoomId(roomId);
        req.setCccdFrontBase64(frontBase64);
        req.setCccdBackBase64(backBase64);
        req.setSelfieBase64(selfieBase64);

        return ResponseEntity.ok(kycService.submitAndVerify(req));
    }
}

