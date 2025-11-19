package com.auctionaa.backend.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserTradeStatsResponse {

    private int role;

    private long purchasedCount;
    private BigDecimal totalSpent;

    private long soldCount;
    private BigDecimal totalRevenue;
}

