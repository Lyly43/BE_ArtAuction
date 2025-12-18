package com.auctionaa.backend.DTO.Response;

import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.Invoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentConfirmResponse {
    private String qrUrl;
    private String note;
    private boolean paid;
    private String message;
}
