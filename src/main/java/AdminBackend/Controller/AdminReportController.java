package AdminBackend.Controller;

import AdminBackend.DTO.Request.UpdateReportRequest;
import AdminBackend.DTO.Response.AdminReportApiResponse;
import AdminBackend.DTO.Response.AdminReportResponse;
import AdminBackend.DTO.Response.AdminReportStatisticsResponse;
import AdminBackend.Service.AdminReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminReportController {

    @Autowired
    private AdminReportService adminReportService;

    @GetMapping("/lay-du-lieu")
    public ResponseEntity<AdminReportApiResponse<List<AdminReportResponse>>> getAllReports() {
        return adminReportService.getAllReports();
    }

    @GetMapping("/tim-kiem")
    public ResponseEntity<AdminReportApiResponse<List<AdminReportResponse>>> searchReports(
            @RequestParam(value = "q", required = false) String searchTerm) {
        return adminReportService.searchReports(searchTerm);
    }

    @GetMapping("/thong-ke")
    public ResponseEntity<AdminReportApiResponse<AdminReportStatisticsResponse>> getReportStatistics() {
        return adminReportService.getReportStatistics();
    }

    @PutMapping("/cap-nhat/{reportId}")
    public ResponseEntity<AdminReportApiResponse<AdminReportResponse>> updateReport(
            @PathVariable String reportId,
            @RequestBody UpdateReportRequest request) {
        return adminReportService.updateReport(reportId, request);
    }

    @DeleteMapping("/xoa/{reportId}")
    public ResponseEntity<AdminReportApiResponse<Void>> deleteReport(@PathVariable String reportId) {
        return adminReportService.deleteReport(reportId);
    }
}


