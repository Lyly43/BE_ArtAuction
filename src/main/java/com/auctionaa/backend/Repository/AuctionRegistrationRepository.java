package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.AuctionRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuctionRegistrationRepository extends MongoRepository<AuctionRegistration, String> {}