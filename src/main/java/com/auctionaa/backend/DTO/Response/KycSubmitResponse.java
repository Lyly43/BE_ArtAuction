package com.auctionaa.backend.DTO.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycSubmitResponse {
    private String registrationId;
    private String kycId;
    private int status; // 1: VERIFIED/ 2: PENDING/ 0: REJECTED
    private Double faceMatchScore;
    private Double livenessScore;
    private String message;
}