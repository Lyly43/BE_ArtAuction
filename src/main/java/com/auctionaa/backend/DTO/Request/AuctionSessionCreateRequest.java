package com.auctionaa.backend.DTO.Request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionSessionCreateRequest {
    private String artworkId;
    private BigDecimal startingPrice;
    private BigDecimal bidStep;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageUrl;
    private int durationMinutes;
}
