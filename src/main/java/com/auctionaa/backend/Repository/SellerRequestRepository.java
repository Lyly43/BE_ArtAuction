package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.SellerRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SellerRequestRepository extends MongoRepository<SellerRequest, String> {
    Optional<SellerRequest> findByUserId(String userId);
    List<SellerRequest> findByStatus(String status);
    List<SellerRequest> findByUserIdOrderByCreatedAtDesc(String userId);
}

