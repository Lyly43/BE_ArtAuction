package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.AuctionRegistrationRequest;
import com.auctionaa.backend.DTO.Request.VerifyPaymentRequest;
import com.auctionaa.backend.DTO.Response.AuctionRegistrationResponse;
import com.auctionaa.backend.DTO.Response.InvoicePaymentConfirmResponse;
import com.auctionaa.backend.DTO.Response.InvoicePaymentResponse;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.AuctionRoomDepositService;
import com.auctionaa.backend.Service.InvoicePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payment/{roomId}")  // => /api/payment/{roomId}/...
@RequiredArgsConstructor
public class PaymentController {

    private final AuctionRoomDepositService depositService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final InvoicePaymentService invoicePaymentService;

//    @PostMapping("/application-fee")
//    public AuctionRegistrationResponse payApplicationFee(
//            @RequestHeader("Authorization") String authHeader,
//            @PathVariable String roomId
//    ) {
//        String userId = jwtUtil.extractUserId(authHeader);
//        userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found!"));
//        return depositService.payApplicationFee(roomId, userId);
//    }
//
//    @PostMapping("/deposit")
//    public AuctionRegistrationResponse payDeposit(
//            @RequestHeader("Authorization") String authHeader,
//            @PathVariable String roomId
//    ) {
//        String userId = jwtUtil.extractUserId(authHeader);
//        userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found!"));
//        return depositService.createQrAndCheck(roomId, userId);
//    }

    @PostMapping("/registration/payment")
    public ResponseEntity<AuctionRegistrationResponse> createPayment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId
    ) {
        String userId = jwtUtil.extractUserId(authHeader);

        AuctionRegistrationResponse resp =
                depositService.createApplicationFeeAndDepositPayment(roomId, userId);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/registration/verify")
    public ResponseEntity<AuctionRegistrationResponse> verifyPayment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId
    ) {
        String userId = jwtUtil.extractUserId(authHeader);

        AuctionRegistrationResponse resp =
                depositService.verifyApplicationFeeAndDepositPayment(roomId, userId);

        return ResponseEntity.ok(resp);
    }



}
