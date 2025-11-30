package AdminBackend.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFilterRequest {
    // Payment Status: null = bỏ qua filter (lấy tất cả)
    // 0 = Pending, 1 = Paid, 2 = Failed
    // Lưu ý: "Overdue" thường được xử lý bằng paymentStatus = 0 (Pending) và có thể kèm điều kiện thời gian
    // Frontend có thể gửi paymentStatus = 2 cho "Overdue" nếu muốn filter theo Failed
    private Integer paymentStatus;
    
    // Invoice Status: null = bỏ qua filter (lấy tất cả)
    // 0 = created, 1 = confirmed, 2 = completed, 3 = cancelled
    private Integer invoiceStatus;
    
    // Total Amount range - có thể dùng preset hoặc custom range
    // Preset: "<1M", "1M-10M", ">10M", hoặc null
    private String totalAmountRange;
    
    // Custom amount range (nếu không dùng preset)
    private BigDecimal totalAmountMin;
    private BigDecimal totalAmountMax;
    
    // Date range: Lọc theo orderDate của invoice
    private LocalDateTime dateFrom; // null = bỏ qua filter
    private LocalDateTime dateTo;   // null = bỏ qua filter
}

