package com.auctionaa.backend.DTO.Request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Data
@Setter
@Getter
public class PlaceBidRequest {
    private BigDecimal amount;
    private String idempotencyKey;
}
