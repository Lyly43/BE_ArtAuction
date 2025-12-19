package com.auctionaa.backend.DTO.Request;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String email;
    private String otp;
}