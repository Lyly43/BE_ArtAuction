package com.auctionaa.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "auction_registrations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionRegistration extends BaseEntity {
    private String userId;
    private String roomId;
    private String kycId;     // tham chiếu KycVerification

    private String status;    // SUBMITTED, VERIFIED, REJECTED
    private String note;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override public String getPrefix() { return "ARTT-"; }
}
