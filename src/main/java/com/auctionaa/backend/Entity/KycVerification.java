package com.auctionaa.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "kyc_verifications")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KycVerification extends BaseEntity {
    private String userId;
    private String roomId;

    // Ảnh (đường dẫn hoặc base64 id lưu tạm - tuỳ bạn)
    private String cccdFrontUrl;
    private String cccdBackUrl;
    private String selfieUrl;

    // Kết quả OCR
    private String fullName;
    private String cccdNumber;
    private LocalDate dateOfBirth;
    private LocalDate issueDate;
    private String addressOnCard;

    // Điểm/flag từ eKYC
    private Double faceMatchScore;   // 0..1
    private Double livenessScore;    // 0..1
    private boolean passed;

    private String rawVendorResponse; // JSON gốc để trace
    private int status; // PENDING, VERIFIED, REJECTED

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override public String getPrefix() { return "KYC-"; }
}
