package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.ArtworkIngestRequest;
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

    public Artwork saveArtwork(String ownerId, ArtworkIngestRequest req, String mainUrl) {
        Artwork aw = new Artwork();
        aw.generateId();
        aw.setOwnerId(ownerId);
        aw.setTitle(req.getTitle());
        aw.setDescription(req.getDescription());
        aw.setAvtArtwork(mainUrl);
        aw.setImageUrls(List.of(mainUrl));
        aw.setStatus(req.getStatus() == null ? 0 : req.getStatus());
        aw.setAiVerified(true); // vì qua Human

        aw.setStartedPrice(req.getStartedPrice() == null ? BigDecimal.ZERO : req.getStartedPrice());
        aw.setPaintingGenre(req.getPaintingGenre());
        aw.setYearOfCreation(req.getYearOfCreation());
        aw.setMaterial(req.getMaterial());
        aw.setSize(req.getSize());
        aw.setCertificateId(req.getCertificateId());

        aw.setCreatedAt(LocalDateTime.now());
        aw.setUpdatedAt(LocalDateTime.now());
        return artworkRepository.save(aw);
    }

    public String uploadAndReturnUrl(MultipartFile image, String folder) throws Exception {
        String publicId = UUID.randomUUID().toString();
        Map res = cloudFolderService.uploadToFolder(image.getBytes(), folder, publicId);
        return (String) res.get("secure_url");
    }
}