package AdminBackend.Service;

import AdminBackend.DTO.Request.AddArtworkRequest;
import AdminBackend.DTO.Request.UpdateArtworkRequest;
import AdminBackend.DTO.Response.AdminArtworkResponse;
import AdminBackend.DTO.Response.ArtworkForSelectionResponse;
import AdminBackend.DTO.Response.ArtworkStatisticsResponse;
import AdminBackend.DTO.Response.UpdateResponse;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminArtworkService {

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Admin thêm tác phẩm mới
     */
    public ResponseEntity<?> addArtwork(AddArtworkRequest request) {
        // Validate ownerId tồn tại
        if (request.getOwnerId() == null || request.getOwnerId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("OwnerId is required");
        }

        Optional<User> ownerOpt = userRepository.findById(request.getOwnerId());
        if (ownerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("User not found with ownerId: " + request.getOwnerId());
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Artwork created successfully with ID: " + savedArtwork.getId());
    }

    /**
     * Lấy tất cả tác phẩm với đầy đủ thông tin
     */
    public ResponseEntity<List<AdminArtworkResponse>> getAllArtworks() {
        List<AdminArtworkResponse> responses = getAllArtworksData();
        return ResponseEntity.ok(responses);
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
                UpdateResponse<Object> errorResponse = new UpdateResponse<>(false, 
                    "Artwork not found with ID: " + artworkId, null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Artwork artwork = artworkOpt.get();

            // Validate ownerId nếu có thay đổi
            if (request.getOwnerId() != null && !request.getOwnerId().equals(artwork.getOwnerId())) {
                Optional<User> ownerOpt = userRepository.findById(request.getOwnerId());
                if (ownerOpt.isEmpty()) {
                    UpdateResponse<Object> errorResponse = new UpdateResponse<>(false, 
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
                true, "Artwork updated successfully", response);
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            UpdateResponse<Object> errorResponse = new UpdateResponse<>(false, 
                "Failed to update artwork: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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
    public ResponseEntity<?> deleteArtwork(String artworkId) {
        Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
        if (artworkOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Artwork not found with ID: " + artworkId);
        }

        artworkRepository.delete(artworkOpt.get());

        return ResponseEntity.ok("Artwork deleted successfully with ID: " + artworkId);
    }

    /**
     * Lấy thống kê tác phẩm
     */
    public ResponseEntity<ArtworkStatisticsResponse> getArtworkStatistics() {
        long totalArtworks = artworkRepository.count();
        long pendingArtworks = artworkRepository.countByStatus(0); // Chưa duyệt
        long approvedArtworks = artworkRepository.countByStatus(1); // Đã duyệt
        long rejectedArtworks = artworkRepository.countByStatus(3); // Từ chối

        ArtworkStatisticsResponse statistics = new ArtworkStatisticsResponse(
                totalArtworks,
                pendingArtworks,
                approvedArtworks,
                rejectedArtworks
        );

        return ResponseEntity.ok(statistics);
    }

    /**
     * Helper method: Map Artwork entity sang AdminArtworkResponse
     */
    private AdminArtworkResponse mapToAdminArtworkResponse(Artwork artwork) {
        AdminArtworkResponse response = new AdminArtworkResponse();
        response.setId(artwork.getId());
        response.setTitle(artwork.getTitle());
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
}

