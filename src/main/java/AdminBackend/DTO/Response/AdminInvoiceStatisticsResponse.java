package AdminBackend.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminInvoiceStatisticsResponse {
    private long totalInvoices;
    private long paidInvoices;
    private long pendingInvoices;
    private long failedInvoices;
}

