package com.auctionaa.backend.DTO.Response;

import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.Invoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@Data
public class InvoicePaymentResponse {
    private Invoice invoice;  // toàn bộ thông tin hóa đơn
    private Optional<Artwork> artwork;
//    private String qrUrl;     // link ảnh QR thanh toán tổng tiền
//    private String note;      // nội dung chuyển khoản (addInfo)
//    private boolean paid;     // đã tìm thấy giao dịch tương ứng chưa
//    private String message;   // message cho FE
}
