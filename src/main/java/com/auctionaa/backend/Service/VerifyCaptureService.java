package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Response.VerifyTopUpResponse;
import com.auctionaa.backend.Entity.MbTxn;
import com.auctionaa.backend.Entity.Wallet;
import com.auctionaa.backend.Entity.WalletTransaction;
import com.auctionaa.backend.Repository.WalletRepository;
import com.auctionaa.backend.Repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerifyCaptureService {

    private final WalletTransactionRepository txnRepo;
    private final WalletRepository walletRepo;
    private final MbClient mbClient;
    private final WebhookService webhookService;

    @Transactional
    public VerifyTopUpResponse verifyAndCapture(String id) {
        WalletTransaction txn = txnRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction không tồn tại"));

        Wallet wallet = walletRepo.findById(txn.getWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet không tồn tại"));

        // Idempotent
        if (txn.getStatus() == 1) {
            return new VerifyTopUpResponse(txn.getId(), 1, wallet.getBankTxnId(), "Đã nạp trước đó");
        }

        // ✅ Timezone chắc chắn
        ZoneId zone = ZoneId.of("Asia/Bangkok");

        // Khoảng thời gian tìm trên MB (nới rộng để chắc chắn)
        LocalDate base = (txn.getCreatedAt() != null ? txn.getCreatedAt().toLocalDate() : LocalDate.now(zone));
        LocalDate from = base.minusDays(1);
        LocalDate to   = LocalDate.now(zone).plusDays(1);

        List<MbTxn> list = mbClient.fetchRecentTransactions(from, to);
        if (list == null) list = Collections.emptyList();
        if (list.isEmpty()) {
            return new VerifyTopUpResponse(txn.getId(), 0, null, "Chưa tìm thấy giao dịch khớp trong lsgd MB");
        }

        // ✅ Chuẩn hóa note: bỏ dấu, bỏ khoảng trắng, bỏ '-', '_' ... chỉ giữ [a-z0-9]
        String noteKey = normalizeKey(txn.getNote());

        // ✅ Amount: nếu txn.getBalance() đã là BigDecimal thì dùng thẳng
        BigDecimal mustAmount = txn.getBalance();
        if (mustAmount == null) {
            return new VerifyTopUpResponse(txn.getId(), 0, null, "Transaction amount không hợp lệ");
        }
        mustAmount = mustAmount.stripTrailingZeros();

        BigDecimal finalMustAmount = mustAmount;
        MbTxn matched = list.stream()
                // chỉ nhận tiền vào
                .filter(t -> {
                    BigDecimal credit = parseMoney(t.getCreditAmount());
                    return credit != null && credit.compareTo(BigDecimal.ZERO) > 0;
                })
                // match amount
                .filter(t -> {
                    BigDecimal credit = parseMoney(t.getCreditAmount());
                    return credit != null && credit.stripTrailingZeros().compareTo(finalMustAmount) == 0;
                })
                // match note (loose)
                .filter(t -> {
                    String combined = safe(t.getDescription()) + " " + safe(t.getAddDescription());
                    String textKey = normalizeKey(combined);
                    return !noteKey.isEmpty() && textKey.contains(noteKey);
                })
                .findFirst()
                .orElse(null);

        if (matched == null) {
            return new VerifyTopUpResponse(txn.getId(), 0, null, "Chưa tìm thấy giao dịch khớp trong lsgd MB");
        }

        // --- Cập nhật trạng thái & số dư ---
        txn.setStatus(1);
        txnRepo.save(txn);

        wallet.setBalance(wallet.getBalance().add(mustAmount));
        wallet.setBankTxnId(matched.getRefNo());
        walletRepo.save(wallet);

        webhookService.sendTopupSucceeded(wallet, txn);

        return new VerifyTopUpResponse(txn.getId(), 1, matched.getRefNo(), "Nạp thành công");
    }

    private static BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) return null;

        // MB có thể trả "7,610" / "7 610" / "VND 7610" => lấy digits
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;

        try {
            return new BigDecimal(digits);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /**
     * Normalize mạnh: lowercase, bỏ dấu tiếng Việt, và bỏ toàn bộ ký tự không phải chữ/số.
     * Ví dụ:
     * - "IV-1500-1700-4059" -> "iv150017004059"
     * - "IVIV209183..."     -> "iviv209183..."
     */
    private static String normalizeKey(String s) {
        if (s == null) return "";
        // bỏ dấu tiếng Việt (nếu có), lowercase, bỏ mọi ký tự đặc biệt (space, _, -, ...)
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return n.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

}