package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Artwork;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Meta;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ArtworkRepository extends MongoRepository<Artwork, String> {

    // Lấy top theo startedPrice (dùng Pageable để LIMIT) + giới hạn thời gian xử lý + batch cursor
    @Meta(maxExecutionTimeMs = 500L, cursorBatchSize = 200)
    List<Artwork> findByOrderByStartedPriceDesc(Pageable pageable);

    // Query theo ownerId (thường chạy rất nhanh nếu có index)
    @Meta(maxExecutionTimeMs = 1000L, cursorBatchSize = 500)
    List<Artwork> findByOwnerId(String ownerId);

    // (giữ API cũ nếu còn dùng – KHÔNG nên gắn @Meta vào derived method không có Pageable)
    List<Artwork> findTop6ByOrderByStartedPriceDesc();

    List<Artwork> findByOwnerIdAndStatus(String ownerId, int status);
}
