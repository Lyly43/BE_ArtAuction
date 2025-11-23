package com.auctionaa.backend.DTO.Response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlaceBidResponse {
    /** 1 = Accepted, 0 = Rejected, -1 = Outbid */
    private int result;
    private BigDecimal currentPrice;
    /** username của leader hiện tại (có thể null nếu chưa có ai) */
    private String leader;
    private String message;
    /** Thời điểm server xử lý lệnh đặt giá */
    private LocalDateTime bidTime;
    /** thời điểm phiên hiện tại dự kiến kết thúc (để FE countdown) */
    private LocalDateTime sessionEndTime;
    /** true nếu phiên vừa được gia hạn tự động */
    private boolean extended;
}
