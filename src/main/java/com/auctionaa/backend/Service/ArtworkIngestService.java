package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.ArtworkIngestRequest;
import com.auctionaa.backend.DTO.Request.FlaskClassifyResponse;
import com.auctionaa.backend.DTO.Response.FlaskPredictResponse;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Repository.ArtworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtworkIngestService {

    @Value("${flask.predict-url}")
    private String predictUrl;

    @Value("${flask.classify-url}")
    private String classifyUrl;

    private final ArtworkRepository artworkRepository;
    private final CloudinaryFolderService cloudFolderService;

    private final WebClient webClient = WebClient.create();

    public FlaskPredictResponse callFlaskPredict(MultipartFile image) {
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("image", new MultipartInputResource(image));

        return webClient.post()
                .uri(predictUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(FlaskPredictResponse.class)
                .block();
    }

    public FlaskClassifyResponse callFlaskClassify(MultipartFile image) {
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("image", new MultipartInputResource(image));

        return webClient.post()
                .uri(classifyUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(FlaskClassifyResponse.class)
                .block();
    }

    /** Tạo tên folder dựa trên tên + userId; nếu không có tên → user_{userId} */
    public String buildUserFolder(String displayNameOrNull, String userId) {
        String base = (displayNameOrNull == null || displayNameOrNull.isBlank())
                ? "user_" + userId
                : (slugify(displayNameOrNull) + "_" + userId);
        return base;
    }

    private String slugify(String s) {
        return s.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9-_]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    // ✅ helper lấy genre từ classify
    private String extractGenreFromClassify(FlaskClassifyResponse classify) {
        if (classify == null) return null;
        if (classify.getTop1() == null) return null;

        String label = classify.getTop1().getLabel();
        if (label == null || label.isBlank()) return null;

        return label.trim();
    }

    /**
     * ✅ SaveArtwork có kèm classify:
     * - ưu tiên paintingGenre từ classify.top1.label
     * - nếu classify null/fail thì fallback về req.getPaintingGenre()
     */
    public Artwork saveArtwork(String ownerId, ArtworkIngestRequest req, String mainUrl, FlaskClassifyResponse classify) {
        Artwork aw = new Artwork();
        aw.generateId();
        aw.setOwnerId(ownerId);

        aw.setTitle(req.getTitle());
        aw.setDescription(req.getDescription());
        aw.setAvtArtwork(mainUrl);
        aw.setImageUrls(List.of(mainUrl));

        aw.setStatus(req.getStatus() == null ? 0 : req.getStatus());
        aw.setAiVerified(true); // qua detector Human

        aw.setStartedPrice(req.getStartedPrice() == null ? BigDecimal.ZERO : req.getStartedPrice());

        // ✅ genre: lấy từ classify nếu có
        String predictedGenre = extractGenreFromClassify(classify);
        if (predictedGenre != null) {
            aw.setPaintingGenre(predictedGenre);
        } else {
            aw.setPaintingGenre(req.getPaintingGenre());
        }

        // NOTE: yearOfCreation của bạn là int primitive
        // Nếu req.getYearOfCreation() là Integer và có thể null thì cần xử lý default (xem NOTE bên dưới)
        aw.setYearOfCreation(req.getYearOfCreation());

        aw.setMaterial(req.getMaterial());
        aw.setSize(req.getSize());
        aw.setCertificateId(req.getCertificateId());

        aw.setCreatedAt(LocalDateTime.now());
        aw.setUpdatedAt(LocalDateTime.now());
        return artworkRepository.save(aw);
    }

    // giữ hàm cũ nếu bạn còn chỗ khác đang gọi
    public Artwork saveArtwork(String ownerId, ArtworkIngestRequest req, String mainUrl) {
        return saveArtwork(ownerId, req, mainUrl, null);
    }

    public String uploadAndReturnUrl(MultipartFile image, String folder) throws Exception {
        String publicId = UUID.randomUUID().toString();
        Map res = cloudFolderService.uploadToFolder(image.getBytes(), folder, publicId);
        return (String) res.get("secure_url");
    }

    public Artwork saveArtworkFromRequest(String userId, ArtworkIngestRequest req, String secureUrl) {
        Artwork a = new Artwork();

        // ✅ lấy từ JWT
        a.setOwnerId(userId);

        // ✅ map fields từ request -> entity
        a.setTitle(req.getTitle());
        a.setDescription(req.getDescription());
        a.setStartedPrice(req.getStartedPrice());

        a.setPaintingGenre(req.getPaintingGenre());     // thể loại
        a.setYearOfCreation(req.getYearOfCreation());   // năm
        a.setMaterial(req.getMaterial());
        a.setSize(req.getSize());
        a.setCertificateId(req.getCertificateId());

        // ✅ ảnh
        a.setAvtArtwork(secureUrl);
        a.setImageUrls(List.of(secureUrl)); // nếu muốn chỉ lưu avt thì có thể để null hoặc emptyList()
        a.setAiVerified(true);

        // ✅ default values
        a.setStatus(0);          // 0: Chưa duyệt

        // BaseEntity
        a.generateId();
        return artworkRepository.save(a);
    }
}
