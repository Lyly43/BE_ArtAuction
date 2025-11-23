package com.auctionaa.backend.Entity;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "auction_rooms")
public class AuctionRoom extends BaseEntity {

    private String adminId; // thay DBRef
    private List<String> memberIds; // thay DBRef
    private Integer viewCount; // so_luot_xem
    private String roomName;
    private String description;
    private String imageAuctionRoom;
    private String type;

    private int status; //1: Đang diễn ra, 2: Sắp diễn ra,0: Đã kết thúc


    @DecimalMin(value = "0.0", inclusive = true)
    @Field(value = "deposit_amount", targetType = FieldType.DECIMAL128)
    private BigDecimal depositAmount;


    // Thời hạn thanh toán sau khi thắng (số ngày)
    private Integer paymentDeadlineDays;

    //check các user đã trả phí hồ sơ
    private List<String> applicationFeePaidUserIds;


    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public String getPrefix() {
        return "ACR-";
    }
}
