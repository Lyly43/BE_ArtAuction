package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.ArtworkIngestRequest;
import com.auctionaa.backend.DTO.Response.FlaskPredictResponse;
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
     *
     * Body: multipart/form-data
     *  - image: File (bắt buộc)
     *  - metadata: JSON (ArtworkCreateRequest) (bắt buộc) — có thể nhận String & parse nếu cần
     */
    @PostMapping(value = "/ingest", consumes = {"multipart/form-data"})
    public ResponseEntity<?> ingest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestPart("image") MultipartFile image,
            @Valid @RequestPart("metadata") String metadataJson
    ) throws JsonProcessingException {
        ArtworkIngestRequest metadata = new ObjectMapper().readValue(metadataJson, ArtworkIngestRequest.class);
        // 1) Lấy userId từ JWT
        String userId;
        try {
            userId = jwtUtil.extractUserId(authHeader);
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Không lấy được userId từ JWT"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "JWT không hợp lệ: " + e.getMessage()
            ));
        }

        // (Tuỳ chọn) Nếu bạn có thể lấy tên người dùng:
        // String displayName = userService.findDisplayNameById(userId);
        String displayName = null; // nếu chưa có thì để null

        // 2) Gọi Flask predict
        FlaskPredictResponse predict = ingestService.callFlaskPredict(image);
        if (predict == null || !Boolean.TRUE.equals(predict.isSuccess())) {
            return ResponseEntity.status(502).body(Map.of(
                    "status", false,
                    "message", "Flask Predict lỗi hoặc không phản hồi hợp lệ"
            ));
        }

        // 3) Nếu AI → trả message
        if ("AI".equalsIgnoreCase(predict.getPrediction())) {
            return ResponseEntity.ok(Map.of(
                    "status", false,
                    "prediction", predict.getPrediction(),
                    "humanProbability", predict.getHuman_probability(),
                    "aiProbability", predict.getAi_probability(),
                    "message", "Ảnh bị phát hiện là AI, không lưu."
            ));
        }

        // 4) Human → đảm bảo folder tồn tại: "{tenNguoiDung}_{userId}" (hoặc "user_{userId}")
        try {
            String folder = ingestService.buildUserFolder(displayName, userId);
            folderService.ensureFolder(folder);
            String secureUrl = ingestService.uploadAndReturnUrl(image, folder);
            if (secureUrl == null) {
                return ResponseEntity.status(500).body(Map.of(
                        "status", false,
                        "message", "Upload Cloudinary thất bại"
                ));
            }

            Artwork saved = ingestService.saveArtwork(userId, metadata, secureUrl);

            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "prediction", predict.getPrediction(),
                    "humanProbability", predict.getHuman_probability(),
                    "aiProbability", predict.getAi_probability(),
                    "message", "Ảnh là Human. Đã upload & lưu metadata.",
                    "cloudinary_url", secureUrl,
                    "folder", folder,
                    "artwork_id", saved.getId(),
                    "artwork", saved
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", false,
                    "message", "Lỗi khi đảm bảo folder/upload/lưu: " + ex.getMessage()
            ));
        }
    }
}