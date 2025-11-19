package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Page<Invoice> findAll(Pageable pageable);

    Page<Invoice> findByUserId(String userId, Pageable pageable);

    List<Invoice> findByUserIdOrderByOrderDateDesc(String userId);
    List<Invoice> findByUserIdAndPaymentStatus(String userId, int paymentStatus);

    List<Invoice> findByArtworkIdInAndPaymentStatus(List<String> artworkIds, int paymentStatus);
}
