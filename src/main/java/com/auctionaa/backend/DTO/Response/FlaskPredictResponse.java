package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlaskPredictResponse {
    private boolean success;
    private String prediction;
    private Double human_probability;
    private Double ai_probability;
    private Double confidence;
}