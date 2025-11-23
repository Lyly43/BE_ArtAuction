package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminInvoiceResponse {
    private String id;
    private String userId;
    private String buyerName;
    private String buyerEmail;
    private String auctionRoomId;
    private String roomName;
    private String sessionId;
    private String artworkId;
    private String artworkTitle;
    private BigDecimal amount;
    private BigDecimal buyerPremium;
    private BigDecimal insuranceFee;
    private BigDecimal salesTax;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private int paymentStatus;
    private int invoiceStatus;
    private LocalDateTime orderDate;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
}

