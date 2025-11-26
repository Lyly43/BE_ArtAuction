package AdminBackend.Service;

import AdminBackend.DTO.Request.AddArtworkRequest;
import AdminBackend.DTO.Request.ArtworkApprovalRequest;
import AdminBackend.DTO.Request.ArtworkRejectionRequest;
import AdminBackend.DTO.Request.UpdateArtworkRequest;
import AdminBackend.DTO.Response.AdminArtworkDetailResponse;
import AdminBackend.DTO.Response.AdminArtworkResponse;
import AdminBackend.DTO.Response.AdminBasicResponse;
import AdminBackend.DTO.Response.ArtworkForSelectionResponse;
import AdminBackend.DTO.Response.ArtworkStatisticsResponse;
import AdminBackend.DTO.Response.MonthlyComparisonResponse;
import AdminBackend.DTO.Response.UpdateResponse;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminArtworkService {

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MonthlyStatisticsService monthlyStatisticsService;

    @Autowired
    private EmailService emailService;

    /**
     * Admin thêm tác phẩm mới
     */
    public ResponseEntity<AdminBasicResponse<AdminArtworkResponse>> addArtwork(AddArtworkRequest request) {
        // Validate ownerId tồn tại
        if (request.getOwnerId() == null || request.getOwnerId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "OwnerId is required", null));
        }

        Optional<User> ownerOpt = userRepository.findById(request.getOwnerId());
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "User not found with ownerId: " + request.getOwnerId(), null));
        }

        // Tạo artwork mới
        Artwork artwork = new Artwork();
        artwork.setOwnerId(request.getOwnerId());
        artwork.setTitle(request.getTitle());
        artwork.setDescription(request.getDescription());
        artwork.setSize(request.getSize());
        artwork.setMaterial(request.getMaterial());
        artwork.setPaintingGenre(request.getPaintingGenre());
        artwork.setStartedPrice(request.getStartedPrice());
        artwork.setAvtArtwork(request.getAvtArtwork());
        artwork.setImageUrls(request.getImageUrls());
        artwork.setYearOfCreation(request.getYearOfCreation());
        artwork.setStatus(0); // Mặc định: Chưa duyệt
        artwork.setAiVerified(false);
        artwork.setCreatedAt(LocalDateTime.now());
        artwork.setUpdatedAt(LocalDateTime.now());
        artwork.generateId();

        Artwork savedArtwork = artworkRepository.save(artwork);

        AdminArtworkResponse response = mapToAdminArtworkResponse(savedArtwork);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminBasicResponse<>(1, "Artwork created successfully", response));
    }

    /**
     * Lấy tất cả tác phẩm với đầy đủ thông tin
     */
    public ResponseEntity<List<AdminArtworkResponse>> getAllArtworks() {
        List<AdminArtworkResponse> responses = getAllArtworksData();
        return ResponseEntity.ok(responses);
    }

    /**
     * Lấy chi tiết tác phẩm theo ID
     */
    public ResponseEntity<?> getArtworkDetail(String artworkId) {
        Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
        if (artworkOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Artwork not found with ID: " + artworkId, null));
        }

        AdminArtworkDetailResponse detail = mapToDetailResponse(artworkOpt.get());
        return ResponseEntity.ok(detail);
    }

    /**
     * Helper method: Lấy tất cả artworks và map sang AdminArtworkResponse
     */
    private List<AdminArtworkResponse> getAllArtworksData() {
        List<Artwork> artworks = artworkRepository.findAll();
        return artworks.stream()
                .map(this::mapToAdminArtworkResponse)
                .collect(Collectors.toList());
    }


    /**
     * Admin cập nhật thông tin tác phẩm
     */
    public ResponseEntity<?> updateArtwork(String artworkId, UpdateArtworkRequest request) {
        try {
            Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
            if (artworkOpt.isEmpty()) {
                UpdateResponse<Object> errorResponse = new UpdateResponse<>(0, 
                    "Artwork not found with ID: " + artworkId, null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Artwork artwork = artworkOpt.get();

            // Validate ownerId nếu có thay đổi
            if (request.getOwnerId() != null && !request.getOwnerId().equals(artwork.getOwnerId())) {
                Optional<User> ownerOpt = userRepository.findById(request.getOwnerId());
                if (ownerOpt.isEmpty()) {
                    UpdateResponse<Object> errorResponse = new UpdateResponse<>(0, 
                        "User not found with ownerId: " + request.getOwnerId(), null);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }
                artwork.setOwnerId(request.getOwnerId());
            }

            // Cập nhật các trường
            if (request.getTitle() != null) {
                artwork.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                artwork.setDescription(request.getDescription());
            }
            if (request.getSize() != null) {
                artwork.setSize(request.getSize());
            }
            if (request.getMaterial() != null) {
                artwork.setMaterial(request.getMaterial());
            }
            if (request.getPaintingGenre() != null) {
                artwork.setPaintingGenre(request.getPaintingGenre());
            }
            if (request.getStartedPrice() != null) {
                artwork.setStartedPrice(request.getStartedPrice());
            }
            if (request.getAvtArtwork() != null) {
                artwork.setAvtArtwork(request.getAvtArtwork());
            }
            if (request.getImageUrls() != null) {
                artwork.setImageUrls(request.getImageUrls());
            }
            if (request.getYearOfCreation() > 0) {
                artwork.setYearOfCreation(request.getYearOfCreation());
            }
            artwork.setStatus(request.getStatus());
            artwork.setUpdatedAt(LocalDateTime.now());

            Artwork updatedArtwork = artworkRepository.save(artwork);
            AdminArtworkResponse response = mapToAdminArtworkResponse(updatedArtwork);

            UpdateResponse<AdminArtworkResponse> successResponse = new UpdateResponse<>(
                1, "Artwork updated successfully", response);
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            UpdateResponse<Object> errorResponse = new UpdateResponse<>(0, 
                "Failed to update artwork: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Admin duyệt tác phẩm: cập nhật startedPrice (nếu có) + status = 1, gửi email cho owner
     */
    public ResponseEntity<?> approveArtwork(String artworkId, ArtworkApprovalRequest request) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElse(null);
        if (artwork == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Artwork not found with ID: " + artworkId, null));
        }
        if (!artwork.isAiVerified()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0,
                            "Artwork cannot be approved because it has not passed AI verification", null));
        }
        if (artwork.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "Artwork is currently in auction and cannot be approved again", null));
        }

        ArtworkApprovalRequest effectiveRequest = request == null ? new ArtworkApprovalRequest() : request;
        if (effectiveRequest.getStartedPrice() != null && effectiveRequest.getStartedPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "startedPrice must be greater than 0", null));
        }

        if (effectiveRequest.getStartedPrice() != null) {
            artwork.setStartedPrice(effectiveRequest.getStartedPrice());
        }

        artwork.setStatus(1); // Approved
        artwork.setUpdatedAt(LocalDateTime.now());

        Artwork saved = artworkRepository.save(artwork);
        AdminArtworkResponse response = mapToAdminArtworkResponse(saved);

        sendArtworkEmail(saved, true, effectiveRequest.getAdminNote(), null);

        return ResponseEntity.ok(new AdminBasicResponse<>(1, "Artwork approved successfully", response));
    }

    /**
     * Admin từ chối tác phẩm: set status = 3 và gửi email báo cho owner
     */
    public ResponseEntity<?> rejectArtwork(String artworkId, ArtworkRejectionRequest request) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElse(null);
        if (artwork == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Artwork not found with ID: " + artworkId, null));
        }

        ArtworkRejectionRequest effectiveRequest = request == null ? new ArtworkRejectionRequest() : request;
        String finalReason;
        if (!artwork.isAiVerified()) {
            finalReason = "Tác phẩm nghệ thuật của bạn chưa được duyệt bởi hệ thống đánh giá của chúng tôi";
        } else {
            if (!StringUtils.hasText(effectiveRequest.getReason())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AdminBasicResponse<>(0, "reason is required when rejecting artwork", null));
            }
            finalReason = effectiveRequest.getReason();
        }

        artwork.setStatus(3); // Rejected
        artwork.setUpdatedAt(LocalDateTime.now());
        Artwork saved = artworkRepository.save(artwork);
        AdminArtworkResponse response = mapToAdminArtworkResponse(saved);

        sendArtworkEmail(saved, false, effectiveRequest.getAdminNote(), finalReason);

        return ResponseEntity.ok(new AdminBasicResponse<>(1, "Artwork rejected successfully", response));
    }


    /**
     * Tìm kiếm tác phẩm theo title, author (username), hoặc id
     */
    public ResponseEntity<List<AdminArtworkResponse>> searchArtworks(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return ResponseEntity.ok(getAllArtworksData());
        }

        String trimmedTerm = searchTerm.trim();
        
        // Tìm artworks theo title hoặc id
        List<Artwork> artworksByTitleOrId = artworkRepository.searchArtworksByTitleOrId(trimmedTerm);
        
        // Tìm các user có username match với searchTerm (để tìm theo author)
        List<User> matchingUsers = userRepository.searchUsers(trimmedTerm);
        List<String> ownerIds = matchingUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());
        
        // Tìm artworks theo ownerId (author)
        List<Artwork> artworksByAuthor = ownerIds.isEmpty() 
            ? List.of() 
            : artworkRepository.findByOwnerIdIn(ownerIds);
        
        // Gộp kết quả và loại bỏ trùng lặp
        List<Artwork> allArtworks = new java.util.ArrayList<>(artworksByTitleOrId);
        for (Artwork artwork : artworksByAuthor) {
            if (!allArtworks.stream().anyMatch(a -> a.getId().equals(artwork.getId()))) {
                allArtworks.add(artwork);
            }
        }
        
        List<AdminArtworkResponse> responses = allArtworks.stream()
                .map(this::mapToAdminArtworkResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Admin xóa tác phẩm
     */
    public ResponseEntity<AdminBasicResponse<Void>> deleteArtwork(String artworkId) {
        Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
        if (artworkOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "Artwork not found with ID: " + artworkId, null));
        }

        artworkRepository.delete(artworkOpt.get());

        return ResponseEntity.ok(new AdminBasicResponse<>(1, "Artwork deleted successfully", null));
    }

    /**
     * Lấy thống kê tác phẩm (bao gồm so sánh tháng này vs tháng trước)
     */
    public ResponseEntity<ArtworkStatisticsResponse> getArtworkStatistics() {
        long totalArtworks = artworkRepository.count();
        long pendingArtworks = artworkRepository.countByStatus(0); // Chưa duyệt
        long approvedArtworks = artworkRepository.countByStatus(1); // Đã duyệt
        long rejectedArtworks = artworkRepository.countByStatus(3); // Từ chối

        // Lấy thống kê so sánh tháng này vs tháng trước
        MonthlyComparisonResponse monthlyComparison = monthlyStatisticsService.getMonthlyComparison("artworks", "createdAt");
        MonthlyComparisonResponse.MonthlyComparisonData compData = monthlyComparison.getData();
        
        // Tạo monthly comparison data
        ArtworkStatisticsResponse.MonthlyComparison monthlyComp = new ArtworkStatisticsResponse.MonthlyComparison(
            compData.getCurrentMonth().getTotal(),
            compData.getPreviousMonth().getTotal(),
            compData.getChange().getAmount(),
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease(),
            compData.getCurrentMonth().getMonth(),
            compData.getPreviousMonth().getMonth()
        );

        ArtworkStatisticsResponse statistics = new ArtworkStatisticsResponse(
                totalArtworks,
                pendingArtworks,
                approvedArtworks,
                rejectedArtworks,
                monthlyComp
        );

        return ResponseEntity.ok(statistics);
    }

    /**
     * Thống kê so sánh tháng này vs tháng trước cho artworks
     */
    public ResponseEntity<MonthlyComparisonResponse> getArtworkMonthlyComparison() {
        MonthlyComparisonResponse response = monthlyStatisticsService.getMonthlyComparison("artworks", "createdAt");
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method: Map Artwork entity sang AdminArtworkResponse
     */
    private AdminArtworkResponse mapToAdminArtworkResponse(Artwork artwork) {
        AdminArtworkResponse response = new AdminArtworkResponse();
        response.setId(artwork.getId());
        response.setTitle(artwork.getTitle());
        response.setDescription(artwork.getDescription());
        response.setYearOfCreation(artwork.getYearOfCreation());
        response.setMaterial(artwork.getMaterial());
        response.setPaintingGenre(artwork.getPaintingGenre());
        response.setSize(artwork.getSize());
        response.setAvtArtwork(artwork.getAvtArtwork());
        response.setStartedPrice(artwork.getStartedPrice());
        response.setStatus(artwork.getStatus());
        response.setCreatedAt(artwork.getCreatedAt());

        // Lấy author từ ownerId (username của User)
        String author = "Unknown";
        if (artwork.getOwnerId() != null) {
            Optional<User> ownerOpt = userRepository.findById(artwork.getOwnerId());
            if (ownerOpt.isPresent()) {
                author = ownerOpt.get().getUsername() != null 
                    ? ownerOpt.get().getUsername() 
                    : "Unknown";
            }
        }
        response.setAuthor(author);

        return response;
    }

    private AdminArtworkDetailResponse mapToDetailResponse(Artwork artwork) {
        AdminArtworkDetailResponse detail = new AdminArtworkDetailResponse();
        detail.setId(artwork.getId());
        detail.setOwnerId(artwork.getOwnerId());
        detail.setTitle(artwork.getTitle());
        detail.setDescription(artwork.getDescription());
        detail.setPaintingGenre(artwork.getPaintingGenre());
        detail.setMaterial(artwork.getMaterial());
        detail.setSize(artwork.getSize());
        detail.setYearOfCreation(artwork.getYearOfCreation());
        detail.setCertificateId(artwork.getCertificateId());
        detail.setStartedPrice(artwork.getStartedPrice());
        detail.setAvtArtwork(artwork.getAvtArtwork());
        detail.setImageUrls(artwork.getImageUrls());
        detail.setStatus(artwork.getStatus());
        detail.setAiVerified(artwork.isAiVerified());
        detail.setCreatedAt(artwork.getCreatedAt());
        detail.setUpdatedAt(artwork.getUpdatedAt());
        if (StringUtils.hasText(artwork.getOwnerId())) {
            userRepository.findById(artwork.getOwnerId()).ifPresent(owner -> {
                AdminArtworkDetailResponse.OwnerInfo info = new AdminArtworkDetailResponse.OwnerInfo(
                        owner.getId(),
                        owner.getUsername(),
                        owner.getEmail(),
                        owner.getPhonenumber(),
                        owner.getStatus()
                );
                detail.setOwner(info);
            });
        }
        return detail;
    }

    /**
     * Tìm kiếm tác phẩm để chọn cho phòng đấu giá
     * Hỗ trợ filter theo paintingGenre, material và search theo tên/id/tác giả
     */
    public ResponseEntity<List<ArtworkForSelectionResponse>> searchArtworksForSelection(
            String paintingGenre, String material, String searchTerm) {
        
        List<Artwork> artworks;
        
        // Nếu có paintingGenre, filter theo genre trước
        if (paintingGenre != null && !paintingGenre.trim().isEmpty()) {
            artworks = artworkRepository.findByPaintingGenreContainingIgnoreCase(paintingGenre.trim());
        } else {
            artworks = artworkRepository.findAll();
        }
        
        // Nếu có material, filter thêm theo material
        if (material != null && !material.trim().isEmpty()) {
            String materialTrimmed = material.trim();
            artworks = artworks.stream()
                    .filter(artwork -> artwork.getMaterial() != null && 
                            artwork.getMaterial().toLowerCase().contains(materialTrimmed.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Nếu có searchTerm, filter thêm theo tên/id/tác giả
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String trimmedTerm = searchTerm.trim();
            
            // Tìm theo title hoặc id
            List<Artwork> byTitleOrId = artworkRepository.searchArtworksByTitleOrId(trimmedTerm);
            
            // Tìm theo author (username)
            List<User> matchingUsers = userRepository.searchUsers(trimmedTerm);
            List<String> ownerIds = matchingUsers.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            List<Artwork> byAuthor = ownerIds.isEmpty() 
                ? List.of() 
                : artworkRepository.findByOwnerIdIn(ownerIds);
            
            // Gộp kết quả
            List<Artwork> searchResults = new java.util.ArrayList<>(byTitleOrId);
            for (Artwork artwork : byAuthor) {
                if (!searchResults.stream().anyMatch(a -> a.getId().equals(artwork.getId()))) {
                    searchResults.add(artwork);
                }
            }
            
            // Intersect với danh sách đã filter theo genre
            artworks = artworks.stream()
                    .filter(artwork -> searchResults.stream()
                            .anyMatch(sr -> sr.getId().equals(artwork.getId())))
                    .collect(Collectors.toList());
        }
        
        // Map sang ArtworkForSelectionResponse
        List<ArtworkForSelectionResponse> responses = artworks.stream()
                .map(this::mapToArtworkForSelectionResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Helper method: Map Artwork entity sang ArtworkForSelectionResponse
     */
    private ArtworkForSelectionResponse mapToArtworkForSelectionResponse(Artwork artwork) {
        ArtworkForSelectionResponse response = new ArtworkForSelectionResponse();
        response.setId(artwork.getId());
        response.setTitle(artwork.getTitle());
        response.setDescription(artwork.getDescription());
        response.setPaintingGenre(artwork.getPaintingGenre());
        response.setMaterial(artwork.getMaterial());
        response.setSize(artwork.getSize());
        response.setAvtArtwork(artwork.getAvtArtwork());
        response.setStartedPrice(artwork.getStartedPrice());
        response.setStatus(artwork.getStatus());

        // Lấy author từ ownerId (username của User)
        String author = "Unknown";
        if (artwork.getOwnerId() != null) {
            Optional<User> ownerOpt = userRepository.findById(artwork.getOwnerId());
            if (ownerOpt.isPresent()) {
                author = ownerOpt.get().getUsername() != null 
                    ? ownerOpt.get().getUsername() 
                    : "Unknown";
            }
        }
        response.setAuthor(author);

        return response;
    }

    private void sendArtworkEmail(Artwork artwork, boolean approved, String adminNote, String reason) {
        if (!StringUtils.hasText(artwork.getOwnerId())) {
            log.warn("Artwork {} has no ownerId, skip sending email", artwork.getId());
            return;
        }

        Optional<User> ownerOpt = userRepository.findById(artwork.getOwnerId());
        if (ownerOpt.isEmpty()) {
            log.warn("Owner not found for artwork {}", artwork.getId());
            return;
        }

        User owner = ownerOpt.get();
        if (!StringUtils.hasText(owner.getEmail())) {
            log.warn("Owner {} has no email, skip sending notification", owner.getId());
            return;
        }

        String userName = StringUtils.hasText(owner.getUsername()) ? owner.getUsername() : "Bạn";

        try {
            if (approved) {
                emailService.sendArtworkApprovalEmail(owner.getEmail(), userName, artwork, adminNote);
            } else {
                emailService.sendArtworkRejectionEmail(owner.getEmail(), userName, artwork, reason, adminNote);
            }
        } catch (Exception ex) {
            log.error("Failed to send artwork {} email to {}", approved ? "approval" : "rejection", owner.getEmail(), ex);
        }
    }
}

