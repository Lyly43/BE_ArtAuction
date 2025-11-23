package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Reports;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ReportsRepository extends MongoRepository<Reports, String> {

    @Query("{ $or: [ " +
            "{ 'id': { $regex: ?0, $options: 'i' } }, " +
            "{ 'reportReason': { $regex: ?0, $options: 'i' } }, " +
            "{ 'objectId': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Reports> searchReports(String searchTerm);

    long countByReportStatus(int status);

    long countByReportStatusIn(Collection<Integer> statuses);
}


