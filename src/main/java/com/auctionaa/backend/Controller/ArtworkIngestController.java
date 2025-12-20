package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.ArtworkIngestRequest;
import com.auctionaa.backend.DTO.Request.FlaskClassifyResponse;
import com.auctionaa.backend.DTO.Response.FlaskPredictResponse;
import com.auctionaa.backend.DTO.Response.GenreScore;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Service.ArtworkIngestService;
import com.auctionaa.backend.Service.CloudinaryFolderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkIngestController {

    private final ArtworkIngestService ingestService;
    private final CloudinaryFolderService folderService;
    private final JwtUtil jwtUtil;

    /**
     * - Lấy userId từ JWT (Authorization header).
     * - Gọi Flask /predict.
     * - Nếu AI -> trả message.
     * - Nếu Human -> đảm bảo folder user tồn tại, upload, lưu Artwork.
     * <p>
     * Body: multipart/form-data
     * - image: File (bắt buộc)
     * - metadata: JSON (ArtworkCreateRequest) (bắt buộc) — có thể nhận String & parse nếu cần
     */
    @PostMapping(value = "/ingest", consumes = {"multipart/form-data"})
    public ResponseEntity<?> ingest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestPart("image") MultipartFile image,
            @Valid @RequestPart("metadata") String metadataJson
    ) throws JsonProcessingException {

        // 1) Parse metadata
        ArtworkIngestRequest metadata = new ObjectMapper()
                .readValue(metadataJson, ArtworkIngestRequest.class);

        // 2) JWT -> userId
        String userId;
        try {
            userId = jwtUtil.extractUserId(authHeader);
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", false,
                        "message", "Không lấy được userId từ JWT"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", false,
                    "message", "JWT không hợp lệ: " + e.getMessage()
            ));
        }

        // (Tuỳ chọn) nếu bạn có displayName thì lấy từ DB, không thì để null
        String displayName = null;

        try {
            // 3) Upload Cloudinary (giữ logic folder)
            String folder = ingestService.buildUserFolder(displayName, userId);
            folderService.ensureFolder(folder);

            String secureUrl = ingestService.uploadAndReturnUrl(image, folder);
            if (secureUrl == null) {
                return ResponseEntity.status(500).body(Map.of(
                        "status", false,
                        "message", "Upload Cloudinary thất bại"
                ));
            }

            // 4) Save artwork: lưu dữ liệu từ request + gán ownerId = userId từ JWT
            Artwork saved = ingestService.saveArtworkFromRequest(userId, metadata, secureUrl);

            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "message", "Đã upload ảnh & lưu metadata.",
                    "cloudinary_url", secureUrl,
                    "artwork_id", saved.getId(),
                    "artwork", saved
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", false,
                    "message", "Lỗi upload/lưu: " + ex.getMessage()
            ));
        }
    }
}