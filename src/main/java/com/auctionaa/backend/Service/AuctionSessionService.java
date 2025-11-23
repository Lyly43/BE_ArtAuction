package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.Entity.AuctionSession;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AuctionSessionService {

    private final MongoTemplate mongoTemplate;

    public AuctionSessionService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Tìm kiếm và lọc auction session (history) của user hiện tại
     * - Tìm theo ID
     * - Tìm theo auctionRoomId (tên phòng - thông qua ID)
     * - Lọc theo type (thể loại)
     * - Lọc theo startTime (ngày)
     * - Filter theo sellerId hoặc winnerId (user là seller hoặc winner)
     */
    public List<AuctionSession> searchAndFilter(BaseSearchRequest request, String userId) {
        Query query = new Query();
        Criteria criteria = new Criteria();

        // 1. Tìm kiếm theo ID (exact match) - ưu tiên cao nhất
        if (StringUtils.hasText(request.getId())) {
            criteria.and("_id").is(request.getId());
            query.addCriteria(criteria);
            List<AuctionSession> results = mongoTemplate.find(query, AuctionSession.class);
            // Vẫn filter theo userId ngay cả khi tìm theo ID
            if (StringUtils.hasText(userId)) {
                results = results.stream()
                        .filter(session -> userId.equals(session.getSellerId()) || userId.equals(session.getWinnerId()))
                        .toList();
            }
            return results;
        }

        // 2. Filter theo userId: user là seller hoặc winner
        if (StringUtils.hasText(userId)) {
            Criteria userCriteria = new Criteria().orOperator(
                    Criteria.where("sellerId").is(userId),
                    Criteria.where("winnerId").is(userId));
            criteria.andOperator(userCriteria);
        }

        // 3. Lọc theo thể loại/type
        if (StringUtils.hasText(request.getType())) {
            criteria.and("type").is(request.getType());
        }

        // 4. Lọc theo ngày (dateFrom và dateTo)
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            Criteria dateCriteria = new Criteria("startTime");

            if (request.getDateFrom() != null) {
                LocalDateTime dateFrom = request.getDateFrom().atStartOfDay();
                dateCriteria.gte(dateFrom);
            }

            if (request.getDateTo() != null) {
                LocalDateTime dateTo = request.getDateTo().atTime(LocalTime.MAX);
                dateCriteria.lte(dateTo);
            }

            criteria.andOperator(dateCriteria);
        }

        // Áp dụng criteria vào query
        if (criteria.getCriteriaObject().size() > 0) {
            query.addCriteria(criteria);
        } else {
            // Nếu không có điều kiện nào nhưng có userId, vẫn filter theo userId
            if (StringUtils.hasText(userId)) {
                Criteria userCriteria = new Criteria().orOperator(
                        Criteria.where("sellerId").is(userId),
                        Criteria.where("winnerId").is(userId));
                query.addCriteria(userCriteria);
            } else {
                // Nếu không có điều kiện nào và không có userId, trả về tất cả
                return mongoTemplate.findAll(AuctionSession.class);
            }
        }

        // Thực hiện query
        return mongoTemplate.find(query, AuctionSession.class);
    }
}
