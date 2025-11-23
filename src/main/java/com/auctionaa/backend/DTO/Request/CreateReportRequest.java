package com.auctionaa.backend.DTO.Request;

import lombok.Data;

/**
 * Request DTO cho tạo report
 * Sử dụng form-data (MultipartFile)
 */
@Data
public class CreateReportRequest {
    private String reportType; // Loại tố cáo: String ("Spam", "Fake Identity", "Copyright Violation", etc.)
    private String reportedEntityId; // ID của user/artwork/room được tố cáo: String
    private String reason; // Mô tả chi tiết: String (optional - có thể null nếu chỉ có ảnh)
    // Note: MultipartFile sẽ được nhận trực tiếp trong Controller với @RequestPart
}
