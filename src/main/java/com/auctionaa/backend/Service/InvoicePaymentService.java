package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Response.InvoicePaymentResponse;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Repository.InvoiceRepository;
import com.auctionaa.backend.Service.MbClient;
import com.auctionaa.backend.Config.MbProps;
import com.auctionaa.backend.Entity.MbTxn;
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

@Service
@RequiredArgsConstructor
public class InvoicePaymentService {

    private final InvoiceRepository invoiceRepository;
    private final MbClient mbClient;
    private final MbProps mbProps;

    /**
     * Thanh toán hóa đơn:
     * - Lấy hóa đơn theo id
     * - Tạo QR VietQR với totalAmount
     * - Check giao dịch MB khớp amount + note
     * - Nếu khớp => set paymentStatus = 1, lưu lại
     * - Trả về Invoice + QR + note + trạng thái.
     */
    public InvoicePaymentResponse payInvoice(String Id, String userIdFromToken) {
        Invoice invoice = invoiceRepository.findById(Id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found"
                ));

        // Optional: đảm bảo hóa đơn thuộc về user hiện tại
        if (invoice.getUserId() != null && !invoice.getUserId().equals(userIdFromToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thanh toán hóa đơn này");
        }

        BigDecimal amount = invoice.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("totalAmount của hóa đơn không hợp lệ");
        }

        // Tạo nội dung chuyển khoản (note)
        String note = generateInvoiceNote(Id, invoice.getUserId());

        // Tạo link ảnh QR VietQR
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s",
                url(mbProps.getBankCode()),
                url(mbProps.getAccountNo()),
                url(amount.toPlainString()),
                url(note)
        );

        boolean paid = hasMatchingTransaction(amount, note);

        String message;
        if (paid) {
            invoice.setPaymentStatus(1);                 // 1 = paid
            invoice.setPaymentMethod("BANK_TRANSFER");    // ví dụ
            invoice.setPaymentDate(LocalDateTime.now());
            invoice.setPaymentQr(qrUrl);
            // có thể update invoiceStatus nếu muốn, ví dụ:
            // invoice.setInvoiceStatus(2); // completed
            invoiceRepository.save(invoice);
            message = "Thanh toán hóa đơn thành công.";
        } else {
            // Lưu lại QR để FE có thể hiển thị
            invoice.setPaymentQr(qrUrl);
            invoiceRepository.save(invoice);
            message = "Chưa tìm thấy giao dịch tương ứng. Vui lòng chuyển khoản theo QR và chờ hệ thống xác nhận.";
        }

        return new InvoicePaymentResponse(invoice, qrUrl, note, paid, message);
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

        // IV = Invoice
        return "IV-" + invoiceSuffix + "-" + userSuffix + "-" + last4;
    }

    private String url(String s) {
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

        if (txns == null || txns.isEmpty()) {
            return false;
        }

        return txns.stream().anyMatch(tx -> {
            String credit = tx.getCreditAmount();
            if (credit == null) return false;

            // TODO: chỉnh theo kiểu dữ liệu thật của creditAmount
            try {
                BigDecimal creditAmount = new BigDecimal(credit);
                if (creditAmount.compareTo(amount) != 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }

            String desc = tx.getDescription();
            return desc != null && desc.contains(note);
        });
    }
}

