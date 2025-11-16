package com.auctionaa.backend.DTO.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkIngestRequest {
    @NotBlank
    private String title;

    private String description;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal startedPrice;

    @Min(0)
    private int yearOfCreation;

    // tuỳ chọn giữ/loại các field này
    private String paintingGenre;
    private String material;
    private String size;
    private String certificateId;
    private Integer status; // default 0
}