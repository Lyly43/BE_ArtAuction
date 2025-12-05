package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Page<Invoice> findAll(Pageable pageable);

    Page<Invoice> findByUserId(String userId, Pageable pageable);

    Optional<Invoice> findTopByUserIdOrderByOrderDateDesc(String userId);

    Page<Invoice> findByUserIdOrderByOrderDateDesc(String userId, Pageable pageable);


    List<Invoice> findByUserIdAndPaymentStatus(String userId, int paymentStatus);

    List<Invoice> findByArtworkIdInAndPaymentStatus(List<String> artworkIds, int paymentStatus);


    Optional<Invoice> findById(String id);

    List<Invoice> findByUserId(String userId);
    
    // Search methods for admin - tìm kiếm theo id, userId, artworkTitle, artistName, roomName, winnerName, transactionId
    @Query("{ $or: [ " +
           "{ 'id': { $regex: ?0, $options: 'i' } }, " +
           "{ 'userId': { $regex: ?0, $options: 'i' } }, " +
           "{ 'artworkTitle': { $regex: ?0, $options: 'i' } }, " +
           "{ 'artistName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'roomName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'winnerName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'transactionId': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Invoice> searchInvoices(String searchTerm);
    
    // Count methods for statistics
    long countByInvoiceStatus(int status);
    
    long countByInvoiceStatusIn(Collection<Integer> statuses);
}

