package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.AuctionRegistrationRequest;
import com.auctionaa.backend.DTO.Response.AuctionRegistrationResponse;
import com.auctionaa.backend.DTO.Response.InvoicePaymentResponse;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.AuctionRoomDepositService;
import com.auctionaa.backend.Service.InvoicePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/{roomId}")  // => /api/payment/{roomId}/...
@RequiredArgsConstructor
public class PaymentController {

    private final AuctionRoomDepositService depositService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final InvoicePaymentService invoicePaymentService;

    // 🔹 Thanh toán CỌC
    @PostMapping("/deposit")
    public AuctionRegistrationResponse payDeposit(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId
    ) {
        // Lấy userId từ JWT
        String userId = jwtUtil.extractUserId(authHeader);

        // Đảm bảo user tồn tại (optional nhưng hợp lý)
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        // GỌI SERVICE: không dùng req nữa, chỉ roomId + userId
        return depositService.createQrAndCheck(roomId, userId);
    }

    // 🔹 Thanh toán PHÍ HỒ SƠ (100k)
    @PostMapping("/application-fee")
    public AuctionRegistrationResponse payApplicationFee(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId
    ) {
        String userId = jwtUtil.extractUserId(authHeader);

        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        return depositService.payApplicationFee(roomId, userId);
    }

    // 🔹 Thanh toán COMBO: PHÍ HỒ SƠ + CỌC
    @PostMapping("/application-fee-and-deposit")
    public AuctionRegistrationResponse payApplicationFeeAndDeposit(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId
    ) {
        String userId = jwtUtil.extractUserId(authHeader);

        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        return depositService.payApplicationFeeAndDeposit(roomId, userId);
    }

    @PostMapping("/{invoiceId}/pay-invoice")
    public InvoicePaymentResponse payInvoice(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String invoiceId
    ) {
        String userId = jwtUtil.extractUserId(authHeader);
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        return invoicePaymentService.payInvoice(invoiceId, userId);
    }
}
