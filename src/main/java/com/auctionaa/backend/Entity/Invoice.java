package com.auctionaa.backend.Entity;

import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "invoices")
public class Invoice extends BaseEntity {

    @Indexed
    private String auctionRoomId;
    @Indexed
    private String userId;
    @Indexed
    private String artworkId;

    @Indexed
    private String sessionId;
    // Snapshot nhanh
    private String artworkTitle;
    private String artworkImageUrl;
    private String artistName;
    private String roomName;

    // Winner info
    private String winnerName;
    private String winnerEmail;

    // Giao dịch & chi phí
    private BigDecimal amount;
    private BigDecimal buyerPremium;
    private BigDecimal insuranceFee;
    private BigDecimal salesTax;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;

    private String paymentMethod;
    private int paymentStatus; // 0=pending, 1=paid, 2=failed
    private String transactionId;
    private int transactionType;

    // Vận chuyển
    private String shippingAddress;
    private String recipientNameText;
    private String recipientPhone;
    private String note;
    private int deliveryStatus; // 0=pending, 1=shipped, 2=delivered

    // Quản lý
    private int invoiceStatus; // 0=created, 1=confirmed, 2=completed, 3=cancelled

    private String paymentQr;

    @Indexed
    private LocalDateTime paymentDate;
    @Indexed
    private LocalDateTime orderDate;

    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public String getPrefix() { return "IV-"; }
}

