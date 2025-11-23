package com.auctionaa.backend.Service;

import com.auctionaa.backend.Constants.ReportConstants;
import com.auctionaa.backend.DTO.Request.CreateReportRequest;
import com.auctionaa.backend.DTO.Response.ReportResponse;
import com.auctionaa.backend.Entity.Reports;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.ReportsRepository;
import com.auctionaa.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportsRepository reportsRepository;
    private final UserRepository userRepository;
    private final ArtworkRepository artworkRepository;
    private final AuctionRoomRepository auctionRoomRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Tạo report mới
     *
     * @param entityType Loại entity: int (1=User, 2=Artwork, 3=Auction Room, 4=AI
     *                   Artwork)
     * @param request    Request với reportType (String), reportedEntityId, reason
     * @param imageFile  Ảnh chứng minh (optional)
     * @param reporterId ID người tố cáo (từ JWT token)
     * @return ReportResponse
     */
    public ReportResponse createReport(int entityType, CreateReportRequest request,
                                       MultipartFile imageFile, String reporterId) {
        // 1. Validate reportType
        if (!ReportConstants.isValidReportType(entityType, request.getReportType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid complaint type for " + ReportConstants.getEntityTypeName(entityType));
        }

        // 2. Validate reason và image
        // Đối với AI Artwork: cả reason và image đều bắt buộc
        // Đối với các loại khác: phải có ít nhất reason hoặc image
        if (entityType == ReportConstants.ENTITY_AI_ARTWORK) {
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Description is required for AI reporting");
            }
            if (imageFile == null || imageFile.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Photo ID is required for AI reporting");
            }
        } else {
            // Các loại khác: phải có ít nhất reason hoặc image
            if ((request.getReason() == null || request.getReason().trim().isEmpty())
                    && (imageFile == null || imageFile.isEmpty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Must have at least a description or photo to prove it");
            }
        }

        // 3. Validate reportedEntityId tồn tại (trừ AI Artwork)
        if (entityType != ReportConstants.ENTITY_AI_ARTWORK) {
            validateReportedEntity(entityType, request.getReportedEntityId());
        }

        // 4. Validate không được tố cáo chính mình
        if (entityType == ReportConstants.ENTITY_USER
                && request.getReportedEntityId().equals(reporterId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot denounce himself");
        }

        // 5. Kiểm tra duplicate (cùng user, cùng entity, cùng type trong 24h)
        // (Có thể bỏ qua nếu muốn cho phép tố cáo nhiều lần)

        // 6. Tạo report entity
        Reports report = new Reports();
        report.generateId();
        report.setReportType(request.getReportType());
        report.setEntityType(entityType);
        report.setReportedEntityId(request.getReportedEntityId());
        report.setReporterId(reporterId);
        report.setReason(request.getReason() != null ? request.getReason().trim() : null);
        report.setStatus(ReportConstants.STATUS_PENDING);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        // 7. Upload ảnh nếu có
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadReportImage(report.getId(),
                        imageFile);
                report.setImageUrl(uploadResult.getUrl());
                report.setImagePublicId(uploadResult.getPublicId());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error uploading photo: " + e.getMessage());
            }
        }

        // 8. Lưu vào database
        Reports savedReport = reportsRepository.save(report);

        // 9. Convert sang Response
        return toResponse(savedReport, "Your complaint has been sent successfully. We will review it as soon as possible.");
    }

    /**
     * Validate reportedEntityId có tồn tại không
     * Lưu ý: Không validate cho AI_ARTWORK vì không cần reportedEntityId
     */
    private void validateReportedEntity(int entityType, String entityId) {
        switch (entityType) {
            case ReportConstants.ENTITY_USER:
                if (!userRepository.existsById(entityId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "User does not exist");
                }
                break;
            case ReportConstants.ENTITY_ARTWORK:
                if (!artworkRepository.existsById(entityId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Artwork does not exist");
                }
                break;
            case ReportConstants.ENTITY_AUCTION_ROOM:
                if (!auctionRoomRepository.existsById(entityId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Auction Room does not exist");
                }
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid entity type");
        }
    }

    /**
     * Lấy danh sách reports của user hiện tại
     */
    public List<ReportResponse> getMyReports(String reporterId) {
        List<Reports> reports = reportsRepository.findByReporterId(reporterId);
        return reports.stream()
                .map(report -> toResponse(report, null))
                .collect(Collectors.toList());
    }

    /**
     * Convert Reports entity sang ReportResponse
     */
    private ReportResponse toResponse(Reports report, String message) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setEntityType(report.getEntityType());
        response.setEntityTypeName(ReportConstants.getEntityTypeName(report.getEntityType()));
        response.setReportType(report.getReportType());
        response.setReportedEntityId(report.getReportedEntityId());
        response.setReporterId(report.getReporterId());
        response.setReason(report.getReason());
        response.setImageUrl(report.getImageUrl());
        response.setStatus(report.getStatus());
        response.setStatusName(ReportConstants.getStatusName(report.getStatus()));
        response.setAdminNote(report.getAdminNote());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());
        response.setResolvedAt(report.getResolvedAt());
        response.setMessage(message);
        return response;
    }
}
