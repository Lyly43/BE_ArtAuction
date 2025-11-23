package com.auctionaa.backend.DTO.Response;

import com.auctionaa.backend.Entity.Invoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Data
public class InvoicePaymentResponse {
    private Invoice invoice;  // toàn bộ thông tin hóa đơn
    private String qrUrl;     // link ảnh QR thanh toán tổng tiền
    private String note;      // nội dung chuyển khoản (addInfo)
    private boolean paid;     // đã tìm thấy giao dịch tương ứng chưa
    private String message;   // message cho FE
}
