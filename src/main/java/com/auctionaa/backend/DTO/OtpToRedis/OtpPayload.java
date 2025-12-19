package com.auctionaa.backend.DTO.OtpToRedis;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpPayload implements Serializable {
    private String otpHash;
    private LocalDateTime createdAt;
}