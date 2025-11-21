package AdminBackend.Service;

import AdminBackend.DTO.Request.UpdateReportRequest;
import AdminBackend.DTO.Response.AdminReportApiResponse;
import AdminBackend.DTO.Response.AdminReportResponse;
import AdminBackend.DTO.Response.AdminReportStatisticsResponse;
import com.auctionaa.backend.Entity.ReportObject;
import com.auctionaa.backend.Entity.Reports;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.ReportObjectRepository;
import com.auctionaa.backend.Repository.ReportsRepository;
import com.auctionaa.backend.Repository.UserRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminReportService {

    @Autowired
    private ReportsRepository reportsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportObjectRepository reportObjectRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    public ResponseEntity<AdminReportApiResponse<List<AdminReportResponse>>> getAllReports() {
        // Đọc trực tiếp từ MongoDB để lấy DBRef
        List<Document> reportDocuments = mongoTemplate.getCollection("reports")
                .find()
                .sort(new Document("createdAt", -1))
                .into(new java.util.ArrayList<>());

        List<AdminReportResponse> data = reportDocuments.stream()
                .map(this::mapDocumentToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AdminReportApiResponse<>(true, "Lấy danh sách báo cáo thành công", data));
    }

    public ResponseEntity<AdminReportApiResponse<List<AdminReportResponse>>> searchReports(String searchTerm) {
        List<Document> reportDocuments;
        if (!StringUtils.hasText(searchTerm)) {
            reportDocuments = mongoTemplate.getCollection("reports")
                    .find()
                    .sort(new Document("createdAt", -1))
                    .into(new java.util.ArrayList<>());
        } else {
            // Tìm kiếm với regex
            Document query = new Document("$or", java.util.Arrays.asList(
                    new Document("_id", new Document("$regex", searchTerm).append("$options", "i")),
                    new Document("reportReason", new Document("$regex", searchTerm).append("$options", "i"))
            ));
            reportDocuments = mongoTemplate.getCollection("reports")
                    .find(query)
                    .sort(new Document("createdAt", -1))
                    .into(new java.util.ArrayList<>());
        }

        List<AdminReportResponse> data = reportDocuments.stream()
                .map(this::mapDocumentToResponse)
                .collect(Collectors.toList());
        String message = StringUtils.hasText(searchTerm)
                ? String.format("Tìm thấy %d báo cáo cho từ khóa '%s'", data.size(), searchTerm)
                : "Lấy danh sách báo cáo thành công";
        return ResponseEntity.ok(new AdminReportApiResponse<>(true, message, data));
    }

    public ResponseEntity<AdminReportApiResponse<AdminReportResponse>> updateReport(
            String reportId,
            UpdateReportRequest request) {
        Optional<Reports> optionalReports = reportsRepository.findById(reportId);
        if (optionalReports.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminReportApiResponse<>(false, "Không tìm thấy báo cáo", null));
        }

        Reports report = optionalReports.get();

        if (StringUtils.hasText(request.getReportReason())) {
            report.setReportReason(request.getReportReason());
        }
        if (request.getReportStatus() != null) {
            report.setReportStatus(request.getReportStatus());
            if (request.getReportStatus() == 2 && report.getReportDoneTime() == null) {
                report.setReportDoneTime(LocalDateTime.now());
            }
        }
        if (request.getReportDoneTime() != null) {
            report.setReportDoneTime(request.getReportDoneTime());
        }

        Reports updated = reportsRepository.save(report);
        // Đọc lại từ MongoDB để lấy DBRef đầy đủ
        Document updatedDoc = mongoTemplate.getCollection("reports")
                .find(new Document("_id", reportId))
                .first();
        AdminReportResponse response = updatedDoc != null 
                ? mapDocumentToResponse(updatedDoc)
                : mapToResponse(updated);
        return ResponseEntity.ok(new AdminReportApiResponse<>(true, "Cập nhật báo cáo thành công", response));
    }

    public ResponseEntity<AdminReportApiResponse<Void>> deleteReport(String reportId) {
        Optional<Reports> optionalReports = reportsRepository.findById(reportId);
        if (optionalReports.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminReportApiResponse<>(false, "Không tìm thấy báo cáo", null));
        }

        reportsRepository.delete(optionalReports.get());
        return ResponseEntity.ok(new AdminReportApiResponse<>(true, "Xóa báo cáo thành công", null));
    }

    public ResponseEntity<AdminReportApiResponse<AdminReportStatisticsResponse>> getReportStatistics() {
        long total = reportsRepository.count();
        long pending = reportsRepository.countByReportStatus(0);
        long investigating = reportsRepository.countByReportStatus(1);
        long resolved = reportsRepository.countByReportStatus(2);

        AdminReportStatisticsResponse stats = new AdminReportStatisticsResponse(total, pending, investigating, resolved);
        return ResponseEntity.ok(new AdminReportApiResponse<>(true, "Thống kê báo cáo", stats));
    }

    /**
     * Map Document từ MongoDB (có DBRef) sang AdminReportResponse
     */
    private AdminReportResponse mapDocumentToResponse(Document doc) {
        String reportId = doc.getString("_id");
        
        // Lấy user từ DBRef (DBRef trong MongoDB là Document với $ref và $id)
        User reporter = null;
        String reporterId = null;
        Object userRef = doc.get("user");
        if (userRef instanceof Document) {
            Document userDbRef = (Document) userRef;
            Object idObj = userDbRef.get("$id");
            if (idObj != null) {
                reporterId = idObj.toString();
                if (reporterId != null && !reporterId.trim().isEmpty()) {
                    reporter = userRepository.findById(reporterId).orElse(null);
                }
            }
        } else if (userRef instanceof String) {
            reporterId = (String) userRef;
            if (reporterId != null && !reporterId.trim().isEmpty()) {
                reporter = userRepository.findById(reporterId).orElse(null);
            }
        }
        
        // Lấy reportobject từ DBRef
        ReportObject reportObject = null;
        String objectId = null;
        String objectName = null;
        Object reportObjectRef = doc.get("reportobject");
        if (reportObjectRef instanceof Document) {
            Document objectDbRef = (Document) reportObjectRef;
            Object idObj = objectDbRef.get("$id");
            if (idObj != null) {
                objectId = idObj.toString();
                if (objectId != null && !objectId.trim().isEmpty()) {
                    reportObject = reportObjectRepository.findById(objectId).orElse(null);
                }
            }
        } else if (reportObjectRef instanceof String) {
            objectId = (String) reportObjectRef;
            if (objectId != null && !objectId.trim().isEmpty()) {
                reportObject = reportObjectRepository.findById(objectId).orElse(null);
            }
        }
        
        // Lấy thông tin object từ ReportObject
        User objectUser = null;
        if (reportObject != null) {
            if (reportObject.getUserId() != null && !reportObject.getUserId().trim().isEmpty()) {
                objectUser = userRepository.findById(reportObject.getUserId()).orElse(null);
                objectName = objectUser != null ? objectUser.getUsername() : null;
            } else if (reportObject.getArtworkId() != null && !reportObject.getArtworkId().trim().isEmpty()) {
                objectName = "Artwork: " + reportObject.getArtworkId();
            } else if (reportObject.getAuctionRoomId() != null && !reportObject.getAuctionRoomId().trim().isEmpty()) {
                objectName = "AuctionRoom: " + reportObject.getAuctionRoomId();
            }
        }
        
        // Lấy các trường khác
        String reportTarget = doc.getString("reportTarget");
        String reportReason = doc.getString("reportReason");
        Object statusObj = doc.get("reportStatus");
        int reportStatus = 0;
        if (statusObj instanceof Number) {
            reportStatus = ((Number) statusObj).intValue();
        } else if (statusObj instanceof String) {
            try {
                reportStatus = Integer.parseInt((String) statusObj);
            } catch (NumberFormatException e) {
                reportStatus = 0;
            }
        }
        
        // Lấy thời gian
        LocalDateTime reportTime = null;
        Object reportTimeObj = doc.get("reportTime");
        if (reportTimeObj instanceof Date) {
            reportTime = ((Date) reportTimeObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (reportTimeObj instanceof Document) {
            Date date = ((Document) reportTimeObj).getDate("$date");
            if (date != null) {
                reportTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        
        LocalDateTime createdAt = null;
        Object createdAtObj = doc.get("createdAt");
        if (createdAtObj instanceof Date) {
            createdAt = ((Date) createdAtObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (createdAtObj instanceof Document) {
            Date date = ((Document) createdAtObj).getDate("$date");
            if (date != null) {
                createdAt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        
        LocalDateTime reportDoneTime = null;
        Object reportDoneTimeObj = doc.get("reportDoneTime");
        if (reportDoneTimeObj instanceof Date) {
            reportDoneTime = ((Date) reportDoneTimeObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (reportDoneTimeObj instanceof Document) {
            Date date = ((Document) reportDoneTimeObj).getDate("$date");
            if (date != null) {
                reportDoneTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        
        // Nếu reportTime null, dùng createdAt
        if (reportTime == null) {
            reportTime = createdAt;
        }

        return new AdminReportResponse(
                reportId,
                reporterId,
                reporter != null ? reporter.getUsername() : null,
                reporter != null ? reporter.getEmail() : null,
                objectId,
                objectName,
                objectUser != null ? objectUser.getEmail() : null,
                reportTarget,
                reportReason,
                reportStatus,
                reportTime,
                createdAt,
                reportDoneTime
        );
    }

    /**
     * Map Reports entity sang AdminReportResponse (fallback nếu không dùng DBRef)
     */
    private AdminReportResponse mapToResponse(Reports report) {
        // Kiểm tra null trước khi gọi findById để tránh lỗi "The given id must not be null"
        User reporter = null;
        if (report.getUserId() != null && !report.getUserId().trim().isEmpty()) {
            reporter = userRepository.findById(report.getUserId()).orElse(null);
        }
        
        User objectUser = null;
        if (report.getObjectId() != null && !report.getObjectId().trim().isEmpty()) {
            objectUser = userRepository.findById(report.getObjectId()).orElse(null);
        }

        return new AdminReportResponse(
                report.getId(),
                report.getUserId(),
                reporter != null ? reporter.getUsername() : null,
                reporter != null ? reporter.getEmail() : null,
                report.getObjectId(),
                objectUser != null ? objectUser.getUsername() : report.getObject(),
                objectUser != null ? objectUser.getEmail() : null,
                null, // reportTarget
                report.getReportReason(),
                report.getReportStatus(),
                report.getCreatedAt(), // reportTime
                report.getCreatedAt(),
                report.getReportDoneTime()
        );
    }
}

