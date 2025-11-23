package com.auctionaa.backend.DTO.Request;

import lombok.Data;

@Data
public class KycVerifyRequest {
    private String cccdFrontBase64;
    private String cccdBackBase64;
    private String selfieBase64;
}

