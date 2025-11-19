package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.KycVerification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface KycVerificationRepository extends MongoRepository<KycVerification, String> {
    // Tìm KYC verification gần nhất của user với roomId = null (profile verification)
    Optional<KycVerification> findFirstByUserIdAndRoomIdIsNullOrderByCreatedAtDesc(String userId);
}