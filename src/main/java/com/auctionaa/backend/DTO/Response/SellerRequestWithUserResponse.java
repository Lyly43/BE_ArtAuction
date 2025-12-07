package com.auctionaa.backend.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SellerRequestWithUserResponse {
    private String requestId;

    private String userId;
    private String userName;

    private String verificationImageUrl;
    private String description;
    private String status;
    private String adminNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
