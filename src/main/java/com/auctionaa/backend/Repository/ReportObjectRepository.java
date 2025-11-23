package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.ReportObject;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReportObjectRepository extends MongoRepository<ReportObject, String> {
}

