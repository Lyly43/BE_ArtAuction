package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Reports;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportsRepository extends MongoRepository<Reports, String> {
    // Tìm tất cả reports của một user (người tố cáo)
    List<Reports> findByReporterId(String reporterId);

    // Tìm tất cả reports của một entity (user/artwork/room được tố cáo)
    List<Reports> findByEntityTypeAndReportedEntityId(int entityType, String reportedEntityId);

    // Tìm reports theo status
    List<Reports> findByStatus(int status);

    // Tìm reports theo entityType và status
    List<Reports> findByEntityTypeAndStatus(int entityType, int status);

    // Kiểm tra xem user đã tố cáo entity này chưa (trong 24h)
    Optional<Reports> findByReporterIdAndEntityTypeAndReportedEntityIdAndReportType(
            String reporterId, int entityType, String reportedEntityId, String reportType);

    @Query("{ $or: [ " +
            "{ 'id': { $regex: ?0, $options: 'i' } }, " +
            "{ 'reportReason': { $regex: ?0, $options: 'i' } }, " +
            "{ 'objectId': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Reports> searchReports(String searchTerm);

    long countByReportStatus(int status);

    long countByReportStatusIn(Collection<Integer> statuses);
}



