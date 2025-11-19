package com.auctionaa.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "seller_requests")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SellerRequest extends BaseEntity {
    private String userId;
    private String verificationImageUrl; // URL ảnh chứng thực (Cloudinary)
    private String description; // Mô tả từ user
    private String status; // PENDING, APPROVED, REJECTED
    private String adminNote; // Ghi chú của admin khi approve/reject

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public String getPrefix() {
        return "SR-";
    }
}

