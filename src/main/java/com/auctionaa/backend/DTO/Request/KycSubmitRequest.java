package com.auctionaa.backend.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycSubmitRequest {
    private String userId;
    private String roomId;
    private String cccdFrontBase64;
    private String cccdBackBase64;
    private String selfieBase64;
}
