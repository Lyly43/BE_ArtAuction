package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Page<Invoice> findAll(Pageable pageable);

    Page<Invoice> findByUserId(String userId, Pageable pageable);

    List<Invoice> findByUserIdOrderByOrderDateDesc(String userId);

    @Query("{ $or: [ " +
            "{ 'id': { $regex: ?0, $options: 'i' } }, " +
            "{ 'userId': { $regex: ?0, $options: 'i' } }, " +
            "{ 'auctionRoomId': { $regex: ?0, $options: 'i' } }, " +
            "{ 'roomName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'sessionId': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Invoice> searchInvoices(String searchTerm);

    long countByInvoiceStatus(int invoiceStatus);

    long countByInvoiceStatusIn(Collection<Integer> invoiceStatuses);
}
