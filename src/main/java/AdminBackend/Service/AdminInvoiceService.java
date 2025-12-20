package AdminBackend.Service;

import AdminBackend.DTO.Request.InvoiceFilterRequest;
import AdminBackend.DTO.Request.UpdateInvoiceRequest;
import AdminBackend.DTO.Response.AdminInvoiceApiResponse;
import AdminBackend.DTO.Response.AdminInvoiceResponse;
import AdminBackend.DTO.Response.AdminInvoiceStatisticsResponse;
import AdminBackend.DTO.Response.MonthlyComparisonResponse;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminInvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private MonthlyStatisticsService monthlyStatisticsService;

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "orderDate");

    public ResponseEntity<AdminInvoiceApiResponse<List<AdminInvoiceResponse>>> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll(DEFAULT_SORT);
        List<AdminInvoiceResponse> data = invoices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(
                new AdminInvoiceApiResponse<>(1, "Lấy danh sách hóa đơn thành công", data)
        );
    }

    public ResponseEntity<AdminInvoiceApiResponse<List<AdminInvoiceResponse>>> searchInvoices(String searchTerm) {
        List<Invoice> invoices;
        if (!StringUtils.hasText(searchTerm)) {
            invoices = invoiceRepository.findAll(DEFAULT_SORT);
        } else {
            invoices = invoiceRepository.searchInvoices(searchTerm);
        }

        List<AdminInvoiceResponse> data = invoices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        String message = StringUtils.hasText(searchTerm)
                ? String.format("Tìm thấy %d hóa đơn cho từ khóa '%s'", data.size(), searchTerm)
                : "Lấy danh sách hóa đơn thành công";

        return ResponseEntity.ok(new AdminInvoiceApiResponse<>(1, message, data));
    }

    /**
     * Lọc hóa đơn theo các tiêu chí
     * Tất cả các trường filter đều optional (null = bỏ qua filter đó)
     */
    public ResponseEntity<AdminInvoiceApiResponse<List<AdminInvoiceResponse>>> filterInvoices(InvoiceFilterRequest request) {
        // Nếu request null, trả về tất cả invoices
        if (request == null) {
            return getAllInvoices();
        }
        
        List<Invoice> allInvoices = invoiceRepository.findAll(DEFAULT_SORT);
        
        List<Invoice> filteredInvoices = allInvoices.stream()
                .filter(invoice -> {
                    // Filter by paymentStatus (null = bỏ qua filter)
                    if (request.getPaymentStatus() != null && invoice.getPaymentStatus() != request.getPaymentStatus()) {
                        return false;
                    }
                    
                    // Filter by invoiceStatus (null = bỏ qua filter)
                    // 0 = created, 1 = confirmed, 2 = completed, 3 = cancelled
                    if (request.getInvoiceStatus() != null && invoice.getInvoiceStatus() != request.getInvoiceStatus()) {
                        return false;
                    }
                    
                    // Filter by totalAmount range - lọc theo totalAmount của invoice
                    BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
                    
                    // Nếu có totalAmountRange preset, dùng preset
                    if (request.getTotalAmountRange() != null && !request.getTotalAmountRange().trim().isEmpty()) {
                        String range = request.getTotalAmountRange().trim().toLowerCase();
                        BigDecimal oneMillion = new BigDecimal("1000000");
                        BigDecimal tenMillion = new BigDecimal("10000000");
                        
                        switch (range) {
                            case "<1m":
                                if (totalAmount.compareTo(oneMillion) >= 0) {
                                    return false;
                                }
                                break;
                            case "1m-10m":
                                if (totalAmount.compareTo(oneMillion) < 0 || 
                                    totalAmount.compareTo(tenMillion) > 0) {
                                    return false;
                                }
                                break;
                            case ">10m":
                                if (totalAmount.compareTo(tenMillion) <= 0) {
                                    return false;
                                }
                                break;
                            default:
                                // Nếu giá trị không hợp lệ, bỏ qua filter này
                                break;
                        }
                    } else {
                        // Nếu không có preset, dùng custom range (totalAmountMin, totalAmountMax)
                        if (request.getTotalAmountMin() != null && totalAmount.compareTo(request.getTotalAmountMin()) < 0) {
                            return false;
                        }
                        if (request.getTotalAmountMax() != null && totalAmount.compareTo(request.getTotalAmountMax()) > 0) {
                            return false;
                        }
                    }
                    
                    // Filter by date range (lọc theo orderDate)
                    if (request.getDateFrom() != null) {
                        if (invoice.getOrderDate() == null || 
                            invoice.getOrderDate().isBefore(request.getDateFrom())) {
                            return false;
                        }
                    }
                    if (request.getDateTo() != null) {
                        if (invoice.getOrderDate() == null || 
                            invoice.getOrderDate().isAfter(request.getDateTo())) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        List<AdminInvoiceResponse> data = filteredInvoices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(new AdminInvoiceApiResponse<>(1, "Lọc hóa đơn thành công", data));
    }

    public ResponseEntity<AdminInvoiceApiResponse<AdminInvoiceResponse>> updateInvoice(
            String invoiceId,
            UpdateInvoiceRequest request) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminInvoiceApiResponse<>(0, "Không tìm thấy hóa đơn", null));
        }

        Invoice invoice = optionalInvoice.get();

        // Làm tròn xuống (floor) tất cả các giá trị tiền về số nguyên (chỉ lấy phần trước dấu phẩy)
        java.math.RoundingMode roundingMode = java.math.RoundingMode.FLOOR;
        if (request.getAmount() != null) invoice.setAmount(request.getAmount().setScale(0, roundingMode));
        if (request.getBuyerPremium() != null) invoice.setBuyerPremium(request.getBuyerPremium().setScale(0, roundingMode));
        if (request.getInsuranceFee() != null) invoice.setInsuranceFee(request.getInsuranceFee().setScale(0, roundingMode));
        if (request.getSalesTax() != null) invoice.setSalesTax(request.getSalesTax().setScale(0, roundingMode));
        if (request.getShippingFee() != null) invoice.setShippingFee(request.getShippingFee().setScale(0, roundingMode));
        if (request.getTotalAmount() != null) invoice.setTotalAmount(request.getTotalAmount().setScale(0, roundingMode));
        if (StringUtils.hasText(request.getPaymentMethod())) invoice.setPaymentMethod(request.getPaymentMethod());
        if (request.getPaymentStatus() != null) invoice.setPaymentStatus(request.getPaymentStatus());
        if (request.getInvoiceStatus() != null) invoice.setInvoiceStatus(request.getInvoiceStatus());
        if (request.getDeliveryStatus() != null) invoice.setDeliveryStatus(request.getDeliveryStatus());
        if (StringUtils.hasText(request.getNote())) invoice.setNote(request.getNote());
        if (request.getPaymentDate() != null) invoice.setPaymentDate(request.getPaymentDate());
        if (StringUtils.hasText(request.getShippingAddress())) invoice.setShippingAddress(request.getShippingAddress());
        if (StringUtils.hasText(request.getRecipientNameText())) invoice.setRecipientNameText(request.getRecipientNameText());
        if (StringUtils.hasText(request.getRecipientPhone())) invoice.setRecipientPhone(request.getRecipientPhone());

        invoice.setUpdatedAt(LocalDateTime.now());
        Invoice updatedInvoice = invoiceRepository.save(invoice);

        return ResponseEntity.ok(
                new AdminInvoiceApiResponse<>(1, "Cập nhật hóa đơn thành công", mapToResponse(updatedInvoice))
        );
    }

    public ResponseEntity<AdminInvoiceApiResponse<Void>> deleteInvoice(String invoiceId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminInvoiceApiResponse<>(0, "Không tìm thấy hóa đơn", null));
        }

        invoiceRepository.delete(optionalInvoice.get());
        return ResponseEntity.ok(new AdminInvoiceApiResponse<>(1, "Xóa hóa đơn thành công", null));
    }

    public ResponseEntity<AdminInvoiceApiResponse<AdminInvoiceStatisticsResponse>> getInvoiceStatistics() {
        long total = invoiceRepository.count();
        long paid = invoiceRepository.countByInvoiceStatus(2);
        long pending = invoiceRepository.countByInvoiceStatusIn(Arrays.asList(0, 1));
        long failed = invoiceRepository.countByInvoiceStatus(3);

        // Lấy thống kê so sánh tháng này vs tháng trước cho số lượng invoice
        MonthlyComparisonResponse monthlyComparison = monthlyStatisticsService.getMonthlyComparison("invoices", "createdAt");
        MonthlyComparisonResponse.MonthlyComparisonData compData = monthlyComparison.getData();
        
        // Tạo monthly comparison data (số lượng invoice)
        AdminInvoiceStatisticsResponse.MonthlyComparison monthlyComp = new AdminInvoiceStatisticsResponse.MonthlyComparison(
            compData.getCurrentMonth().getTotal(),
            compData.getPreviousMonth().getTotal(),
            compData.getChange().getAmount(),
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease(),
            compData.getCurrentMonth().getMonth(),
            compData.getPreviousMonth().getMonth()
        );

        AdminInvoiceStatisticsResponse stats = new AdminInvoiceStatisticsResponse(total, paid, pending, failed, monthlyComp);
        return ResponseEntity.ok(new AdminInvoiceApiResponse<>(1, "Thống kê hóa đơn", stats));
    }

    /**
     * Thống kê so sánh tháng này vs tháng trước cho invoices (doanh thu)
     */
    public ResponseEntity<MonthlyComparisonResponse> getInvoiceMonthlyComparison() {
        MonthlyComparisonResponse response = monthlyStatisticsService.getMonthlyRevenueComparison("invoices", "createdAt", "totalAmount");
        return ResponseEntity.ok(response);
    }

    private AdminInvoiceResponse mapToResponse(Invoice invoice) {
        return new AdminInvoiceResponse(
                invoice.getId(),
                invoice.getUserId(),
                invoice.getWinnerName(),
                invoice.getWinnerEmail(),
                invoice.getAuctionRoomId(),
                invoice.getRoomName(),
                invoice.getSessionId(),
                invoice.getArtworkId(),
                invoice.getArtworkTitle(),
                invoice.getAmount(),
                invoice.getBuyerPremium(),
                invoice.getInsuranceFee(),
                invoice.getSalesTax(),
                invoice.getShippingFee(),
                invoice.getTotalAmount(),
                invoice.getPaymentMethod(),
                invoice.getPaymentStatus(),
                invoice.getInvoiceStatus(),
                invoice.getOrderDate(),
                invoice.getPaymentDate(),
                invoice.getCreatedAt()
        );
    }

}

