package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenreScore {
    private String label;
    private double probability; // hoặc confidence (%)
}
