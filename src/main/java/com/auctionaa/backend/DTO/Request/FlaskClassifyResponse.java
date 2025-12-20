package com.auctionaa.backend.DTO.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlaskClassifyResponse {
    private TopItem top1;
    private List<TopItem> topk;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopItem {
        private Integer index;
        private String label;
        private Double probability;
    }
}
