package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Page<Invoice> findAll(Pageable pageable);

    Page<Invoice> findByUserId(String userId, Pageable pageable);

    Optional<Invoice> findTopByUserIdOrderByOrderDateDesc(String userId);

    List<Invoice> findByUserIdOrderByOrderDateDesc(String userId);

    Optional<Invoice> findById(String id);

    List<Invoice> findByUserId(String userId);
}
