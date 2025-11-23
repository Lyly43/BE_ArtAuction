package com.auctionaa.backend.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SellerRequestResponse {
    private String id;
    private String userId;
    private String verificationImageUrl;
    private String description;
    private String status; // PENDING, APPROVED, REJECTED
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

