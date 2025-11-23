package com.auctionaa.backend.DTO.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycVerifyResponse {
    private String kycId;
    private int kycStatus; // 0 = chưa xác thực, 1 = đã xác thực thành công
    private int role; // Role mới của user sau khi verify
    private Double faceMatchScore;
    private Double livenessScore;
    private String message;
}

