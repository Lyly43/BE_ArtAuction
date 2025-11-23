package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Response.SellerRequestResponse;
import com.auctionaa.backend.Service.SellerRequestService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seller-request")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminSellerRequestController {

    private final SellerRequestService sellerRequestService;

    /**
     * Lấy danh sách request theo status (PENDING, APPROVED, REJECTED)
     */
    @GetMapping
    public ResponseEntity<List<SellerRequestResponse>> getRequestsByStatus(@RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(sellerRequestService.getRequestsByStatus(status));
    }

    /**
     * Admin duyệt request → set role = 2 (seller)
     */
    @PutMapping(value = "/{requestId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SellerRequestResponse> approveRequest(@PathVariable String requestId, @RequestBody(required = false) AdminNoteRequest adminNote) {

        String note = adminNote != null ? adminNote.getNote() : null;
        return ResponseEntity.ok(sellerRequestService.approveSellerRequest(requestId, note));
    }

    /**
     * Admin từ chối request
     */
    @PutMapping(value = "/{requestId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SellerRequestResponse> rejectRequest(@PathVariable String requestId, @RequestBody(required = false) AdminNoteRequest adminNote) {

        String note = adminNote != null ? adminNote.getNote() : null;
        return ResponseEntity.ok(sellerRequestService.rejectSellerRequest(requestId, note));
    }

    @Data
    static class AdminNoteRequest {
        private String note;
    }
}

