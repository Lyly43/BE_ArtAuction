package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuctionRegistrationResponse {
    private String qrUrl;
    private String note;
    private boolean paid;
    private String message;
}
