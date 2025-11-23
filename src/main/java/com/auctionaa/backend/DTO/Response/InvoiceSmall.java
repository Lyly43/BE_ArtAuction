package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSmall {
    private String artworkTitle;
    private String artworkImageUrl;
    private String roomName;
    private String totalAmount;   // từ BigDecimal -> String
    private int paymentStatus; // từ int -> String (Pending/Paid/Failed)
}
