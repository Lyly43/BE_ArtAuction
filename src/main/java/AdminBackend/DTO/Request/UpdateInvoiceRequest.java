package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceRequest {
    private BigDecimal amount;
    private BigDecimal buyerPremium;
    private BigDecimal insuranceFee;
    private BigDecimal salesTax;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private Integer paymentStatus;
    private Integer invoiceStatus;
    private Integer deliveryStatus;
    private String note;
    private LocalDateTime paymentDate;
    private String shippingAddress;
    private String recipientNameText;
    private String recipientPhone;
}

