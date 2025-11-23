package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.CreateInvoiceRequest;
import com.auctionaa.backend.DTO.Response.InvoiceListItemDTO;
import com.auctionaa.backend.DTO.Response.InvoicePaymentResponse;
import com.auctionaa.backend.DTO.Response.InvoiceSmall;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Repository.InvoiceRepository;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.InvoicePaymentService;
import com.auctionaa.backend.Service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final JwtUtil jwtUtil;
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;
    private final InvoicePaymentService invoicePaymentService;
    private  final InvoiceRepository invoiceRepository;

    // Admin/list: có phân trang
    @GetMapping("/list")
    public Page<Invoice> getAllInvoice(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"));
        return invoiceService.getAllInvoice(pageable);
    }

    @PostMapping("/add")
    public Invoice saveInvoice(@RequestBody CreateInvoiceRequest req) {
        return invoiceService.createInvoice(req);
    }

    // FE hiện tại: trả mảng đơn giản (không Page)
    @GetMapping("/my-invoice")
    public List<InvoiceListItemDTO> getMyInvoices(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractUserId(token);
        return invoiceService.getMyInvoicesArray(email);
    }

    @GetMapping("/my-invoice/small-latest")
    public InvoiceSmall getMyLatestInvoiceSmall(
            @RequestHeader("Authorization") String authHeader
    ) {
        String userId = jwtUtil.extractUserId(authHeader);

        // Optional: đảm bảo user tồn tại
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        return invoiceService.getLatestInvoiceSmallForUser(userId);
    }

    @GetMapping("/my-invoices/latest")
    public List<Invoice> GetMyInvoices(@RequestHeader("Authorization") String authHeader){
        String userId = jwtUtil.extractUserId(authHeader);
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));
        return invoiceRepository.findByUserId(userId);
    }

    @PostMapping("/{Id}/pay-invoice")
    public InvoicePaymentResponse payInvoice(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String Id
    ) {
        String userId = jwtUtil.extractUserId(authHeader);
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));
        return invoicePaymentService.payInvoice(Id, userId);
    }
}
