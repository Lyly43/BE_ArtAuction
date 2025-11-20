package AdminBackend.Service;

import AdminBackend.DTO.Request.UpdateReportRequest;
import AdminBackend.DTO.Response.AdminReportApiResponse;
import AdminBackend.DTO.Response.AdminReportResponse;
import AdminBackend.DTO.Response.AdminReportStatisticsResponse;
import com.auctionaa.backend.Entity.Reports;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.ReportsRepository;
import com.auctionaa.backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminReportService {

    @Autowired
    private ReportsRepository reportsRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    public ResponseEntity<AdminReportApiResponse<List<AdminReportResponse>>> getAllReports() {
        List<Reports> reports = reportsRepository.findAll(DEFAULT_SORT);
        List<AdminReportResponse> data = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AdminReportApiResponse<>(true, "Lấy danh sách báo cáo thành công", data));
    }

    public ResponseEntity<AdminReportApiResponse<List<AdminReportResponse>>> searchReports(String searchTerm) {
        List<Reports> reports;
        if (!StringUtils.hasText(searchTerm)) {
            reports = reportsRepository.findAll(DEFAULT_SORT);
        } else {
            reports = reportsRepository.searchReports(searchTerm);
        }

        List<AdminReportResponse> data = reports.stream()
                .map(this::mapToResponse)
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
        return ResponseEntity.ok(new AdminReportApiResponse<>(true, "Cập nhật báo cáo thành công", mapToResponse(updated)));
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

    private AdminReportResponse mapToResponse(Reports report) {
        User reporter = userRepository.findById(report.getUserId()).orElse(null);
        User objectUser = userRepository.findById(report.getObjectId()).orElse(null);

        return new AdminReportResponse(
                report.getId(),
                report.getUserId(),
                reporter != null ? reporter.getUsername() : null,
                reporter != null ? reporter.getEmail() : null,
                report.getObjectId(),
                objectUser != null ? objectUser.getUsername() : report.getObject(),
                objectUser != null ? objectUser.getEmail() : null,
                report.getReportReason(),
                report.getReportStatus(),
                report.getCreatedAt(),
                report.getReportDoneTime()
        );
    }
}

