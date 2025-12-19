package com.auctionaa.backend.DTO.OtpToRedis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegisterPayload {
    private String email;
    private String username;
    private String passwordHash;
    private String phone;
    private long createdAtMillis;
}