package com.auctionaa.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reports")
public class Reports extends BaseEntity {

    // ========== INT FIELDS ==========
    private int entityType; // Loại entity: int (1=User, 2=Artwork, 3=Auction Room, 4=AI Artwork)
    private int status; // Trạng thái: int (0=Pending, 1=Processing, 2=Resolved, 3=Rejected)

    // ========== STRING FIELDS ==========
    private String reportType; // Loại tố cáo: String ("Spam", "Fake Identity", "Copyright Violation", etc.)
    private String reportedEntityId; // ID của user/artwork/room được tố cáo: String
    private String reporterId; // ID người tố cáo (từ JWT token): String
    private String reason; // Mô tả chi tiết: String (optional - có thể null nếu chỉ có ảnh)
    private String imageUrl; // URL ảnh chứng minh từ Cloudinary: String (optional)
    private String imagePublicId; // Public ID của ảnh trên Cloudinary: String (để xóa sau)
    private String adminNote; // Ghi chú của admin: String (optional)

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt; // Thời gian xử lý xong

    @Override
    public String getPrefix() {
        return "RP-";
    }
}
