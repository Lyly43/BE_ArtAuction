package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.KycVerification;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface KycVerificationRepository extends MongoRepository<KycVerification, String> {}