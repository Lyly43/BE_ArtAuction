package com.auctionaa.backend.Controller;

import com.auctionaa.backend.Constants.ReportConstants;
import com.auctionaa.backend.DTO.Request.CreateReportRequest;
import com.auctionaa.backend.DTO.Response.ReportResponse;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final JwtUtil jwtUtil;

    /**
     * Tố cáo User
     * POST /api/reports/user
     * Content-Type: multipart/form-data
     *
     * Form data:
     * - reportType: String - Loại tố cáo ("Spam", "Fake Identity", "Scam / Fraud",
     * etc.) - Bắt buộc
     * - reportedEntityId: String - ID của user bị tố cáo - Bắt buộc
     * - reason: String - Mô tả chi tiết - Optional (nhưng phải có ít nhất reason
     * hoặc image)
     * - image: MultipartFile - Ảnh chứng minh - Optional (nhưng phải có ít nhất
     * reason hoặc image)
     */
    @PostMapping("/user")
    public ResponseEntity<ReportResponse> reportUser(
            @RequestParam("reportType") String reportType,
            @RequestParam("reportedEntityId") String reportedEntityId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {

        String reporterId = jwtUtil.extractUserId(authHeader);

        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(reportType);
        request.setReportedEntityId(reportedEntityId);
        request.setReason(reason);

        ReportResponse response = reportService.createReport(
                ReportConstants.ENTITY_USER, request, image, reporterId);

        return ResponseEntity.ok(response);
    }

    /**
     * Tố cáo Artwork
     * POST /api/reports/artwork
     * Content-Type: multipart/form-data
     *
     * Form data:
     * - reportType: String - Loại tố cáo ("Fake Artwork", "Copyright Violation",
     * etc.) - Bắt buộc
     * - reportedEntityId: String - ID của artwork bị tố cáo - Bắt buộc
     * - reason: String - Mô tả chi tiết - Optional
     * - image: MultipartFile - Ảnh chứng minh - Optional
     */
    @PostMapping("/artwork")
    public ResponseEntity<ReportResponse> reportArtwork(
            @RequestParam("reportType") String reportType,
            @RequestParam("reportedEntityId") String reportedEntityId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {

        String reporterId = jwtUtil.extractUserId(authHeader);

        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(reportType);
        request.setReportedEntityId(reportedEntityId);
        request.setReason(reason);

        ReportResponse response = reportService.createReport(
                ReportConstants.ENTITY_ARTWORK, request, image, reporterId);

        return ResponseEntity.ok(response);
    }

    /**
     * Tố cáo Auction Room
     * POST /api/reports/auction-room
     * Content-Type: multipart/form-data
     *
     * Form data:
     * - reportType: String - Loại tố cáo ("Fraudulent Bidding / Fake Bids", etc.) -
     * Bắt buộc
     * - reportedEntityId: String - ID của auction room bị tố cáo - Bắt buộc
     * - reason: String - Mô tả chi tiết - Optional
     * - image: MultipartFile - Ảnh chứng minh - Optional
     */
    @PostMapping("/auction-room")
    public ResponseEntity<ReportResponse> reportAuctionRoom(
            @RequestParam("reportType") String reportType,
            @RequestParam("reportedEntityId") String reportedEntityId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {

        String reporterId = jwtUtil.extractUserId(authHeader);

        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(reportType);
        request.setReportedEntityId(reportedEntityId);
        request.setReason(reason);

        ReportResponse response = reportService.createReport(
                ReportConstants.ENTITY_AUCTION_ROOM, request, image, reporterId);

        return ResponseEntity.ok(response);
    }

    /**
     * Tố cáo kết quả AI không chính xác
     * POST /api/reports/ai-artwork
     * Content-Type: multipart/form-data
     *
     * Form data:
     * - reason: String - Mô tả chi tiết - Bắt buộc
     * - image: MultipartFile - Ảnh chứng minh - Bắt buộc
     *
     * Lưu ý: Không cần truyền reportedEntityId và reportType (tự động = "Inaccurate
     * AI Result")
     */
    @PostMapping("/ai-artwork")
    public ResponseEntity<ReportResponse> reportAIArtwork(
            @RequestParam("reason") String reason,
            @RequestPart("image") MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {

        String reporterId = jwtUtil.extractUserId(authHeader);

        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportConstants.AI_INACCURATE_RESULT); // Tự động = "Inaccurate AI Result"
        request.setReportedEntityId(null); // Không cần ID tranh
        request.setReason(reason);

        ReportResponse response = reportService.createReport(
                ReportConstants.ENTITY_AI_ARTWORK, request, image, reporterId);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách tố cáo của user hiện tại
     * GET /api/reports/my-reports
     */
    @GetMapping("/my-reports")
    public ResponseEntity<List<ReportResponse>> getMyReports(
            @RequestHeader("Authorization") String authHeader) {

        String reporterId = jwtUtil.extractUserId(authHeader);
        List<ReportResponse> reports = reportService.getMyReports(reporterId);

        return ResponseEntity.ok(reports);
    }
}
