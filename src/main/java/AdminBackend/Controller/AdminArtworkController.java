package AdminBackend.Controller;

import AdminBackend.DTO.Request.AddArtworkRequest;
import AdminBackend.DTO.Request.ArtworkApprovalRequest;
import AdminBackend.DTO.Request.ArtworkFilterRequest;
import AdminBackend.DTO.Request.ArtworkRejectionRequest;
import AdminBackend.DTO.Request.UpdateArtworkRequest;
import AdminBackend.DTO.Response.AdminArtworkResponse;
import AdminBackend.DTO.Response.ArtworkForSelectionResponse;
import AdminBackend.DTO.Response.ArtworkStatisticsResponse;
import AdminBackend.Service.AdminArtworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/artworks")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminArtworkController {

    @Autowired
    private AdminArtworkService adminArtworkService;

    /**
     * POST /api/admin/artworks/them-tac-pham
     * Admin thêm tác phẩm mới
     */
    @PostMapping("/them-tac-pham")
    public ResponseEntity<?> addArtwork(@RequestBody AddArtworkRequest request) {
        return adminArtworkService.addArtwork(request);
    }

    /**
     * GET /api/admin/artworks/lay-du-lieu-tac-pham
     * Lấy tất cả tác phẩm với đầy đủ thông tin
     */
    @GetMapping("/lay-du-lieu-tac-pham")
    public ResponseEntity<List<AdminArtworkResponse>> getAllArtworks() {
        return adminArtworkService.getAllArtworks();
    }

    /**
     * PUT /api/admin/artworks/cap-nhat-tac-pham/{artworkId}
     * Admin cập nhật thông tin tác phẩm
     */
    @PutMapping("/cap-nhat-tac-pham/{artworkId}")
    public ResponseEntity<?> updateArtwork(
            @PathVariable String artworkId,
            @RequestBody UpdateArtworkRequest request) {
        return adminArtworkService.updateArtwork(artworkId, request);
    }

    /**
     * POST /api/admin/artworks/approve/{artworkId}
     * Admin duyệt tác phẩm (set status = 1, cập nhật startedPrice)
     */
    @PostMapping("/approve/{artworkId}")
    public ResponseEntity<?> approveArtwork(
            @PathVariable String artworkId,
            @RequestBody ArtworkApprovalRequest request) {
        return adminArtworkService.approveArtwork(artworkId, request);
    }

    /**
     * POST /api/admin/artworks/reject/{artworkId}
     * Admin từ chối tác phẩm (set status = 3)
     */
    @PostMapping("/reject/{artworkId}")
    public ResponseEntity<?> rejectArtwork(
            @PathVariable String artworkId,
            @RequestBody ArtworkRejectionRequest request) {
        return adminArtworkService.rejectArtwork(artworkId, request);
    }

    /**
     * GET /api/admin/artworks/{artworkId}
     * Lấy chi tiết đầy đủ của một tác phẩm theo ID
     */
    @GetMapping("/{artworkId}")
    public ResponseEntity<?> getArtworkDetail(@PathVariable String artworkId) {
        return adminArtworkService.getArtworkDetail(artworkId);
    }

    /**
     * DELETE /api/admin/artworks/xoa-tac-pham/{artworkId}
     * Admin xóa tác phẩm
     */
    @DeleteMapping("/xoa-tac-pham/{artworkId}")
    public ResponseEntity<?> deleteArtwork(@PathVariable String artworkId) {
        return adminArtworkService.deleteArtwork(artworkId);
    }

    /**
     * GET /api/admin/artworks/thong-ke-tac-pham
     * Lấy thống kê tác phẩm (tổng, chưa duyệt, đã duyệt, từ chối)
     */
    @GetMapping("/thong-ke-tac-pham")
    public ResponseEntity<ArtworkStatisticsResponse> getArtworkStatistics() {
        return adminArtworkService.getArtworkStatistics();
    }


    /**
     * GET /api/admin/artworks/tim-kiem-tac-pham?q={searchTerm}
     * Tìm kiếm tác phẩm theo title, author (username), hoặc id
     */
    @GetMapping("/tim-kiem-tac-pham")
    public ResponseEntity<List<AdminArtworkResponse>> searchArtworks(
            @RequestParam(value = "q", required = false) String searchTerm) {
        return adminArtworkService.searchArtworks(searchTerm);
    }

    /**
     * POST /api/admin/artworks/loc-tac-pham
     * Lọc tác phẩm theo các tiêu chí: paintingGenre, priceRange, status
     * Yêu cầu: Content-Type: application/json
     */
    @PostMapping(value = "/loc-tac-pham", consumes = "application/json")
    public ResponseEntity<List<AdminArtworkResponse>> filterArtworks(@RequestBody ArtworkFilterRequest request) {
        return adminArtworkService.filterArtworks(request);
    }

    /**
     * GET /api/admin/artworks/chon-tac-pham?paintingGenre={genre}&material={material}&q={searchTerm}
     * Tìm kiếm tác phẩm để chọn cho phòng đấu giá
     * - Filter theo paintingGenre (thể loại)
     * - Filter theo material (chất liệu)
     * - Search theo tên, id, hoặc tác giả
     */
    @GetMapping("/chon-tac-pham")
    public ResponseEntity<List<ArtworkForSelectionResponse>> searchArtworksForSelection(
            @RequestParam(value = "paintingGenre", required = false) String paintingGenre,
            @RequestParam(value = "material", required = false) String material,
            @RequestParam(value = "q", required = false) String searchTerm) {
        return adminArtworkService.searchArtworksForSelection(paintingGenre, material, searchTerm);
    }
}
