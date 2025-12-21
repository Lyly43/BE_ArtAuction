package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Wallet;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends MongoRepository<Wallet, String> {
    Optional<Wallet> findById(String id);
    Optional<Wallet> findByUserId(String userId);
    
    // Batch query for optimization - load multiple wallets at once
    List<Wallet> findByUserIdIn(List<String> userIds);

    boolean existsByUserId(String id);
}