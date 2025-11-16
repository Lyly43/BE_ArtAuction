package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Bids;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.auctionaa.backend.Entity.Bids;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BidsRepository extends MongoRepository<Bids, String> {

    // ✅ Lấy bid cao nhất (giá lớn nhất) trong một phiên đấu giá
    Optional<Bids> findTopByAuctionSessionIdOrderByAmountAtThatTimeDesc(String auctionSessionId);

    // ✅ Lấy bid mới nhất của user trong 1 phiên (theo thời gian đặt)
    Optional<Bids> findTopByAuctionSessionIdAndUserIdOrderByBidTimeDesc(String auctionSessionId, String userId);
}
