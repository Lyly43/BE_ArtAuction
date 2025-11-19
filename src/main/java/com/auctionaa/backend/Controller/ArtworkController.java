package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Response.ArtworkResponse;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.ArtworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/artwork")
public class ArtworkController {
    @Autowired
    private ArtworkService artworkService;

    @Autowired
    private JwtUtil jwtUtil;

    // Lấy danh sách 6 tác phẩm có giá cao nhất
    @GetMapping("/featured")
    public List<ArtworkResponse> getFeaturedArtworks() {
        return artworkService.getFeaturedArtworks().stream()
                .map(artworkService::toResponse)
                .toList();
    }

    @GetMapping("/all")
    public List<ArtworkResponse> getAll() {
        return artworkService.getAllArtwork().stream()
                .map(artworkService::toResponse)
                .toList();
    }

    @GetMapping("/my-artworks")
    public List<ArtworkResponse> getMyArtworks(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractUserId(token);
        return artworkService.getByOwnerEmail(email).stream()
                .map(artworkService::toResponse)
                .toList();
    }

    @GetMapping("/by-session/{sessionId}")
    public ResponseEntity<Artwork> getArtworkBySessionId(@PathVariable String sessionId) {
        Artwork artwork = artworkService.getArtworkBySessionId(sessionId);
        return ResponseEntity.ok(artwork);
    }

    // Upload ảnh và tạo Artwork
    // http://localhost:8081/upload Post
    @PostMapping("/upload")
    public Artwork uploadArtwork(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("startedPrice") BigDecimal startedPrice,
            @RequestParam("paintingGenre") String paintingGenre,
            @RequestParam("yearOfCreation") int yearOfCreation,
            @RequestParam("material") String material,
            @RequestParam("size") String size,
            @RequestPart(value = "avtFile", required = false) MultipartFile avtFile, // ảnh chính
            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles, // ảnh phụ
            @RequestHeader("Authorization") String authHeader) throws IOException {

        // Lấy email từ token
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractUserId(token);

        // Gán thông tin Artwork
        Artwork artwork = new Artwork();
        artwork.setTitle(title);
        artwork.setDescription(description);
        artwork.setStartedPrice(startedPrice);
        artwork.setPaintingGenre(paintingGenre);
        artwork.setYearOfCreation(yearOfCreation);
        artwork.setMaterial(material);
        artwork.setSize(size);

        // Tạo artwork với avatar + nhiều ảnh phụ
        return artworkService.createArtworkWithImages(artwork, avtFile, imageFiles, email);
    }

    /**
     * Tìm kiếm và lọc artwork
     * Query params: id, name (title), type (paintingGenre), dateFrom, dateTo
     */
    @GetMapping("/search")
    public List<Artwork> searchAndFilter(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        com.auctionaa.backend.DTO.Request.BaseSearchRequest request = new com.auctionaa.backend.DTO.Request.BaseSearchRequest();
        request.setId(id);
        request.setName(name);
        request.setType(type);

        if (dateFrom != null && !dateFrom.isEmpty()) {
            try {
                request.setDateFrom(java.time.LocalDate.parse(dateFrom));
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }

        if (dateTo != null && !dateTo.isEmpty()) {
            try {
                request.setDateTo(java.time.LocalDate.parse(dateTo));
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }

        return artworkService.searchAndFilter(request);
    }

}
