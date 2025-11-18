package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.SellerRequestDTO;
import com.auctionaa.backend.DTO.Response.SellerRequestResponse;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.SellerRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/seller-request")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class SellerRequestController {

    private final SellerRequestService sellerRequestService;
    private final JwtUtil jwtUtil;

    /**
     * Buyer gửi request lên seller
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SellerRequestResponse> submitSellerRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart("verificationImage") MultipartFile verificationImage,
            @RequestPart("description") String description) {

        String userId = jwtUtil.extractUserId(authHeader);
        SellerRequestDTO dto = new SellerRequestDTO();
        dto.setVerificationImage(verificationImage);
        dto.setDescription(description);
        return ResponseEntity.ok(sellerRequestService.submitSellerRequest(userId, dto));
    }

    /**
     * Lấy danh sách request của user
     */
    @GetMapping("/my-requests")
    public ResponseEntity<List<SellerRequestResponse>> getMyRequests(
            @RequestHeader("Authorization") String authHeader) {

        String userId = jwtUtil.extractUserId(authHeader);
        return ResponseEntity.ok(sellerRequestService.getUserRequests(userId));
    }
}
