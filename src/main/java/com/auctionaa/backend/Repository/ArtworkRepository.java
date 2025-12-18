package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Artwork;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Meta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ArtworkRepository extends MongoRepository<Artwork, String> {

    // Lấy top theo startedPrice (dùng Pageable để LIMIT) + giới hạn thời gian xử lý + batch cursor
    @Meta(maxExecutionTimeMs = 500L, cursorBatchSize = 200)
    List<Artwork> findByOrderByStartedPriceDesc(Pageable pageable);

    // Query theo ownerId (thường chạy rất nhanh nếu có index)
    @Meta(maxExecutionTimeMs = 1000L, cursorBatchSize = 500)
    List<Artwork> findByOwnerId(String ownerId);

//    Optional<Artwork> findById(String Id);

    // (giữ API cũ nếu còn dùng – KHÔNG nên gắn @Meta vào derived method không có Pageable)
    List<Artwork> findTop6ByOrderByStartedPriceDesc();

    List<Artwork> findByOwnerIdAndStatus(String ownerId, int status);
    
    // Tìm artworks theo status
    List<Artwork> findByStatus(int status);
    
    // Count methods for statistics
    long countByStatus(int status);
    
    // Search methods for admin - tìm kiếm theo title hoặc id
    @Query("{ $or: [ " +
           "{ 'id': { $regex: ?0, $options: 'i' } }, " +
           "{ 'title': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Artwork> searchArtworksByTitleOrId(String searchTerm);
    
    // Tìm artworks theo danh sách ownerIds
    List<Artwork> findByOwnerIdIn(List<String> ownerIds);
    
    // Tìm artworks theo paintingGenre
    List<Artwork> findByPaintingGenre(String paintingGenre);
    
    // Tìm artworks theo paintingGenre (case insensitive)
    List<Artwork> findByPaintingGenreContainingIgnoreCase(String paintingGenre);
    
    // Tìm artworks theo material (case insensitive)
    List<Artwork> findByMaterialContainingIgnoreCase(String material);
}
