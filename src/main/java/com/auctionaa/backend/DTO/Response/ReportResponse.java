package com.auctionaa.backend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO cho Report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String id;
    private int entityType; // Loại entity: int (1=User, 2=Artwork, 3=Auction Room, 4=AI Artwork)
    private String entityTypeName; // Tên loại entity
    private String reportType; // Loại tố cáo: String ("Spam", "Fake Identity", etc.)
    private String reportedEntityId;
    private String reporterId;
    private String reason;
    private String imageUrl;
    private int status;
    private String statusName; // Tên trạng thái
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String message; // Thông báo khi tạo thành công
}
