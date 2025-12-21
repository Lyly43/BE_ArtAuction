package com.auctionaa.backend.Service;

import com.auctionaa.backend.Config.MbProps;
import com.auctionaa.backend.DTO.Request.CreateTopUpRequest;
import com.auctionaa.backend.DTO.Response.CreateTopUpResponse;
import com.auctionaa.backend.Entity.Wallet;
import com.auctionaa.backend.Entity.WalletTransaction;
import com.auctionaa.backend.Repository.WalletRepository;
import com.auctionaa.backend.Repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TopUpService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txnRepo;
    private final MbProps mbProps;

    public CreateTopUpResponse createTopUp(String id, CreateTopUpRequest req) {
        Wallet wallet = walletRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wallet không tồn tại"));

        // ✅ Đảm bảo amount được xử lý đúng (loại bỏ scale thập phân nếu có)
        BigDecimal amount = req.getAmount();
        if (amount == null) {
            throw new IllegalArgumentException("Amount không được để trống");
        }
        // Chuyển về số nguyên (VND không có phần thập phân)
        amount = amount.setScale(0, RoundingMode.HALF_UP);

        // 1️⃣ Tạo transaction PENDING
        WalletTransaction txn = new WalletTransaction();
        txn.setStatus(0); // 0 = PENDING
        txn.setTransactionType(1); // 1 = TOP_UP
        txn.setUserId(wallet.getUserId());
        txn.setWalletId(wallet.getId());
        txn.setBalance(amount);

        String txnId = generateTransactionId();
        txn.setId(txnId);

        // ✅ Note ngắn gọn 12 ký tự (vd: TP3f46_6085)
        String shortNote = generateShortNote(wallet.getId());
        String note = (req.getNote() == null || req.getNote().isBlank()) ? shortNote : req.getNote();
        txn.setNote(note);

        txn = txnRepo.save(txn);

        // 2️⃣ Tạo QR compact2 VietQR - dùng toPlainString() để tránh scientific
        // notation
        String amountStr = amount.toPlainString();
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s%s",
                url(mbProps.getBankCode()),
                url(mbProps.getAccountNo()),
                url(amountStr),
                url(note),
                (mbProps.getAccountName() != null && !mbProps.getAccountName().isBlank())
                        ? "&accountName=" + url(mbProps.getAccountName())
                        : "");

        return new CreateTopUpResponse(txn.getId(), qrUrl, qrUrl, note);
    }

    // 🧩 Hàm tạo note ngắn 12 ký tự
    private String generateShortNote(String walletId) {
        String suffix = walletId.length() > 5 ? walletId.substring(walletId.length() - 5) : walletId;
        String millis = String.valueOf(System.currentTimeMillis());
        String last4 = millis.substring(millis.length() - 4);
        return "TP" + suffix + "_" + last4;
    }

    private String url(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String generateTransactionId() {
        String random = Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 4);
        return "TXN-" + System.currentTimeMillis() + "-" + random;
    }
}
