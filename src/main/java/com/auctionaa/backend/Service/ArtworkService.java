package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.DTO.Response.ArtworkResponse;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.AuctionSession;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final CloudinaryService cloudinaryService; // service tự viết để upload
    private final GenericSearchService genericSearchService;

    @Autowired
    private UserRepository userRepository;

    public ArtworkService(ArtworkRepository artworkRepository, CloudinaryService cloudinaryService,
                          GenericSearchService genericSearchService) {
        this.artworkRepository = artworkRepository;
        this.cloudinaryService = cloudinaryService;
        this.genericSearchService = genericSearchService;
    }

    public List<Artwork> getFeaturedArtworks() {
        return artworkRepository.findByOrderByStartedPriceDesc(PageRequest.of(0, 6));
    }

    /**
     * Lấy 4 tranh có giá khởi điểm cao nhất
     * @return Danh sách 4 artwork có startedPrice cao nhất
     */
    public List<Artwork> getTop4ArtworksByHighestPrice() {
        return artworkRepository.findByOrderByStartedPriceDesc(PageRequest.of(0, 4));
    }

    public List<Artwork> getAllArtwork() {
        return artworkRepository.findAll();
    }

    public List<Artwork> getByOwnerEmail(String email) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return artworkRepository.findByOwnerId(user.getId());
    }

    public Artwork saveArtwork(Artwork artwork) {
        if (artwork.getId() == null) {
            artwork.generateId();
        }
        artwork.setCreatedAt(LocalDateTime.now());
        artwork.setUpdatedAt(LocalDateTime.now());
        return artworkRepository.save(artwork);
    }

    // Hàm mới thêm
    public Artwork createArtworkWithImages(Artwork artwork,
                                           MultipartFile avtFile,
                                           List<MultipartFile> imageFiles,
                                           String ownerEmail) {
        try {
            // Upload avatar
            String avtUrl = null;
            if (avtFile != null && !avtFile.isEmpty()) {
                avtUrl = cloudinaryService.uploadFile(avtFile);
            }

            // Upload list images
            List<String> urls = new ArrayList<>();
            if (imageFiles != null) {
                for (MultipartFile file : imageFiles) {
                    String url = cloudinaryService.uploadFile(file);
                    urls.add(url);
                }
            }

            // Lấy user owner
            User user = userRepository.findById(ownerEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (artwork.getId() == null) {
                artwork.generateId();
            }

            artwork.setOwnerId(user.getId());
            artwork.setAvtArtwork(avtUrl); // ảnh chính diện
            artwork.setImageUrls(urls); // các ảnh còn lại

            artwork.setCreatedAt(LocalDateTime.now());
            artwork.setUpdatedAt(LocalDateTime.now());

            return artworkRepository.save(artwork);

        } catch (IOException e) {
            throw new RuntimeException("Error creating Artwork with images: " + e.getMessage());
        }
    }

    @Autowired
    AuctionSessionRepository auctionSessionRepository;

    public Artwork getArtworkBySessionId(String sessionId) {
        AuctionSession auctionSession = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SessionID not found"));

        String artworkId = auctionSession.getArtworkId();
        return artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artwork not found"));
    }

    @Autowired
    private BidsRepository bidsRepository;

    public ArtworkResponse toResponse(Artwork artwork) {
        // Tìm user, nếu không tìm thấy thì set ownerName là null hoặc "Unknown"
        String ownerName = userRepository.findById(artwork.getOwnerId())
                .map(User::getUsername)
                .orElse("Unknown");

        ArtworkResponse dto = new ArtworkResponse();
        dto.setId(artwork.getId());
        dto.setTitle(artwork.getTitle());
        dto.setDescription(artwork.getDescription());
        dto.setAvtArtwork(artwork.getAvtArtwork());
        dto.setImageUrls(artwork.getImageUrls());
        dto.setStatus(artwork.getStatus());
        dto.setAiVerified(artwork.isAiVerified());
        dto.setStartedPrice(artwork.getStartedPrice());
        dto.setPaintingGenre(artwork.getPaintingGenre());
        dto.setYearOfCreation(artwork.getYearOfCreation());
        dto.setMaterial(artwork.getMaterial());
        dto.setSize(artwork.getSize());
        dto.setCertificateId(artwork.getCertificateId());
        dto.setCreatedAt(artwork.getCreatedAt());
        dto.setUpdatedAt(artwork.getUpdatedAt());
        dto.setOwnerName(ownerName);

        // Lấy bid mới nhất của chính ownerId (chỉ khi ownerId không null)
        if (artwork.getOwnerId() != null && !artwork.getOwnerId().trim().isEmpty()) {
            bidsRepository.findTopByAuctionSessionIdAndUserIdOrderByBidTimeDesc(
                    artwork.getId(),
                    artwork.getOwnerId()).ifPresent(bid -> dto.setMyLatestBidAmount(bid.getAmountAtThatTime()));
        }

        return dto;
    }

    /**
     * Tìm kiếm và lọc artwork
     * - Tìm theo ID
     * - Tìm theo title (tên)
     * - Lọc theo paintingGenre (thể loại)
     * - Lọc theo createdAt (ngày tạo)
     * - Filter theo ownerId (chỉ lấy tranh của user đó)
     *
     * @param request       BaseSearchRequest với các điều kiện tìm kiếm
     * @param userIdOrEmail userId hoặc email từ JWT token
     */
    public List<Artwork> searchAndFilter(BaseSearchRequest request, String userIdOrEmail) {
        // Xử lý cả 2 trường hợp: userId hoặc email
        String ownerIdToSearch = userIdOrEmail;

        // Nếu là email (có chứa @), cần convert sang userId
        if (userIdOrEmail != null && userIdOrEmail.contains("@")) {
            User user = userRepository.findById(userIdOrEmail)
                    .orElse(null);
            if (user != null) {
                ownerIdToSearch = user.getId();
            } else {
                // Nếu không tìm thấy user, trả về empty list
                return new ArrayList<>();
            }
        }

        // Nếu ownerIdToSearch vẫn null hoặc empty, trả về empty list
        if (ownerIdToSearch == null || ownerIdToSearch.isEmpty()) {
            return new ArrayList<>();
        }

        return genericSearchService.searchAndFilter(
                request,
                Artwork.class,
                "_id", // idField
                "title", // nameField
                "paintingGenre", // typeField
                "createdAt", // dateField
                "ownerId", // userIdField
                ownerIdToSearch // userId (đã convert nếu cần)
        );
    }

}
