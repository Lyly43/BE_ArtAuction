package AdminBackend.Controller;

import AdminBackend.DTO.Request.UpdateInvoiceRequest;
import AdminBackend.DTO.Response.AdminInvoiceApiResponse;
import AdminBackend.DTO.Response.AdminInvoiceResponse;
import AdminBackend.DTO.Response.AdminInvoiceStatisticsResponse;
import AdminBackend.Service.AdminInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/invoices")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminInvoiceController {

    @Autowired
    private AdminInvoiceService adminInvoiceService;

    @GetMapping("/lay-du-lieu")
    public ResponseEntity<AdminInvoiceApiResponse<List<AdminInvoiceResponse>>> getAllInvoices() {
        return adminInvoiceService.getAllInvoices();
    }

    @GetMapping("/tim-kiem")
    public ResponseEntity<AdminInvoiceApiResponse<List<AdminInvoiceResponse>>> searchInvoices(
            @RequestParam(value = "q", required = false) String searchTerm) {
        return adminInvoiceService.searchInvoices(searchTerm);
    }

    @GetMapping("/thong-ke")
    public ResponseEntity<AdminInvoiceApiResponse<AdminInvoiceStatisticsResponse>> getInvoiceStatistics() {
        return adminInvoiceService.getInvoiceStatistics();
    }

    @PutMapping("/cap-nhat/{invoiceId}")
    public ResponseEntity<AdminInvoiceApiResponse<AdminInvoiceResponse>> updateInvoice(
            @PathVariable String invoiceId,
            @RequestBody UpdateInvoiceRequest request) {
        return adminInvoiceService.updateInvoice(invoiceId, request);
    }

    @DeleteMapping("/xoa/{invoiceId}")
    public ResponseEntity<AdminInvoiceApiResponse<Void>> deleteInvoice(@PathVariable String invoiceId) {
        return adminInvoiceService.deleteInvoice(invoiceId);
    }
}

