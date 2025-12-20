package com.auctionaa.backend.DTO.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StreamStartRequest {

    @NotBlank(message = "hostId is required")
    private String adminId;

    @NotBlank(message = "roomName is required")
    private String roomName;

    private String description;

    private String imageAuctionRoom;

    private String type; // optional, ví dụ "auction", "live"
    
    // Các trường từ endpoint admin cũ
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedEndTime;
    
    private BigDecimal depositAmount; // Tiền đặt cọc
    
    private Integer paymentDeadlineDays; // Thời hạn thanh toán sau khi thắng (số ngày)
    
    private List<AuctionSessionCreateRequest> sessions;

}
