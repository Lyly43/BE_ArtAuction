package com.auctionaa.backend.Service;

import com.auctionaa.backend.Config.MbProps;
import com.auctionaa.backend.DTO.Response.InvoicePaymentConfirmResponse;
import com.auctionaa.backend.DTO.Response.InvoicePaymentResponse;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Entity.MbTxn;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoicePaymentService {

    private final InvoiceRepository invoiceRepository;
    private final MbClient mbClient;
    private final MbProps mbProps;
    private final ArtworkRepository artworkRepository;

    // ===== Internal DTO to carry validated data =====
    private static class PaymentContext {
        private final Invoice invoice;
        private final Optional<Artwork> artwork;
        private final BigDecimal amount;
        private final String note;

        private PaymentContext(Invoice invoice, Optional<Artwork> artwork, BigDecimal amount, String note) {
            this.invoice = invoice;
            this.artwork = artwork;
            this.amount = amount;
            this.note = note;
        }
    }

    /**
     * Thanh toán hóa đơn:
     * - Validate invoice + owner + amount
     * - Tạo note + QR
     * - Check giao dịch MB khớp amount + note
     * - Update invoice theo paid/pending
     * - Trả response
     */
    // (1) INIT: chỉ tạo note + QR, lưu QR vào invoice
    public InvoicePaymentResponse initPayment(String invoiceId, String userIdFromToken) {
        PaymentContext ctx = buildPaymentContext(invoiceId, userIdFromToken);

        String qrUrl = buildVietQrUrl(ctx.amount, ctx.note);
        saveQrOnly(ctx.invoice, qrUrl);

        return new InvoicePaymentResponse(
                ctx.invoice,
                ctx.artwork
        );
    }

    // (2) CONFIRM: trả về DTO mới có qrUrl
    public InvoicePaymentConfirmResponse confirmPayment(String invoiceId, String userIdFromToken) {
        PaymentContext ctx = buildPaymentContext(invoiceId, userIdFromToken);

        String qrUrl = (ctx.invoice.getPaymentQr() != null && !ctx.invoice.getPaymentQr().isBlank())
                ? ctx.invoice.getPaymentQr()
                : buildVietQrUrl(ctx.amount, ctx.note);

        boolean paid = hasMatchingTransaction(ctx.amount, ctx.note);

        String message;
        if (paid) {
            markPaid(ctx.invoice, qrUrl);
            message = "Xác nhận thanh toán thành công.";
        } else {
            saveQrOnly(ctx.invoice, qrUrl);
            message = "Chưa tìm thấy giao dịch tương ứng. Vui lòng thử lại sau.";
        }

        return new InvoicePaymentConfirmResponse(
                qrUrl,
                ctx.note,
                paid,
                message
        );
    }

    // ================== SPLIT METHODS ==================

    /**
     * 1) Lấy dữ liệu + validate quyền + validate amount + build note + load artwork
     */
    private PaymentContext buildPaymentContext(String invoiceId, String userIdFromToken) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invoiceId is required");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        // đảm bảo hóa đơn thuộc về user hiện tại (nếu invoice có userId)
        if (invoice.getUserId() != null && userIdFromToken != null
                && !invoice.getUserId().equals(userIdFromToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thanh toán hóa đơn này");
        }

        BigDecimal amount = invoice.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totalAmount của hóa đơn không hợp lệ");
        }

        String note = generateInvoiceNote(invoiceId, invoice.getUserId());

        Optional<Artwork> artwork = Optional.empty();
        String artworkId = invoice.getArtworkId();
        if (artworkId != null && !artworkId.isBlank()) {
            artwork = artworkRepository.findById(artworkId);
        }

        return new PaymentContext(invoice, artwork, amount, note);
    }

    /**
     * 2) Tạo ảnh QR VietQR (only build url)
     */
    private String buildVietQrUrl(BigDecimal amount, String note) {
        return String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s",
                url(mbProps.getBankCode()),
                url(mbProps.getAccountNo()),
                url(amount.toPlainString()),
                url(note)
        );
    }

    // ================== UPDATE INVOICE METHODS ==================

    private void markPaid(Invoice invoice, String qrUrl) {
        invoice.setPaymentStatus(1);                 // 1 = paid
        invoice.setPaymentMethod("BANK_TRANSFER");
        invoice.setPaymentDate(LocalDateTime.now());
        invoice.setPaymentQr(qrUrl);
        invoiceRepository.save(invoice);
    }

    private void saveQrOnly(Invoice invoice, String qrUrl) {
        invoice.setPaymentQr(qrUrl);
        invoiceRepository.save(invoice);
    }

    // ================== HELPER METHODS ==================

    private String generateInvoiceNote(String invoiceId, String userId) {
        String invoiceSuffix = (invoiceId != null && invoiceId.length() > 4)
                ? invoiceId.substring(invoiceId.length() - 4)
                : invoiceId;

        String userSuffix = (userId != null && userId.length() > 4)
                ? userId.substring(userId.length() - 4)
                : userId;

        String millis = String.valueOf(System.currentTimeMillis());
        String last4 = millis.substring(millis.length() - 4);

        return "IV-" + invoiceSuffix + "-" + userSuffix + "-" + last4;
    }

    private String url(String s) {
        if (s == null) return "";
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private boolean hasMatchingTransaction(BigDecimal amount, String note) {
        LocalDate today = LocalDate.now();

        List<MbTxn> txns = mbClient.fetchRecentTransactions(
                today.minusDays(1),
                today
        );

        if (txns == null || txns.isEmpty()) return false;

        return txns.stream().anyMatch(tx -> {
            String credit = tx.getCreditAmount();
            if (credit == null) return false;

            try {
                BigDecimal creditAmount = new BigDecimal(credit);
                if (creditAmount.compareTo(amount) != 0) return false;
            } catch (NumberFormatException e) {
                return false;
            }

            String desc = tx.getDescription();
            return desc != null && note != null && desc.contains(note);
        });
    }
}
