package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic service để tìm kiếm và lọc các entity
 * Sử dụng MongoTemplate để build dynamic query
 */
@Service
public class GenericSearchService {

    private final MongoTemplate mongoTemplate;

    public GenericSearchService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Generic method để tìm kiếm và lọc entity
     *
     * @param request BaseSearchRequest với các điều kiện tìm kiếm
     * @param entityClass Class của entity cần tìm
     * @param idField Tên field ID (thường là "_id" hoặc "id")
     * @param nameField Tên field để tìm kiếm theo tên (ví dụ: "title", "roomName", "artworkTitle")
     * @param typeField Tên field để lọc theo thể loại (ví dụ: "type", "paintingGenre")
     * @param dateField Tên field để lọc theo ngày (ví dụ: "createdAt", "paymentDate", "startTime")
     * @return List các entity thỏa mãn điều kiện
     */
    public <T> List<T> searchAndFilter(
            BaseSearchRequest request,
            Class<T> entityClass,
            String idField,
            String nameField,
            String typeField,
            String dateField) {

        Query query = new Query();
        Criteria criteria = new Criteria();

        // 1. Tìm kiếm theo ID (exact match) - ưu tiên cao nhất
        if (StringUtils.hasText(request.getId())) {
            criteria.and(idField).is(request.getId());
            query.addCriteria(criteria);
            List<T> results = mongoTemplate.find(query, entityClass);
            return results.isEmpty() ? new ArrayList<>() : results;
        }

        // 2. Tìm kiếm theo tên (partial match, case-insensitive)
        if (StringUtils.hasText(request.getName()) && StringUtils.hasText(nameField)) {
            criteria.and(nameField).regex(request.getName(), "i"); // "i" = case-insensitive
        }

        // 3. Lọc theo thể loại/type
        if (StringUtils.hasText(request.getType()) && StringUtils.hasText(typeField)) {
            criteria.and(typeField).is(request.getType());
        }

        // 4. Lọc theo ngày (dateFrom và dateTo)
        if ((request.getDateFrom() != null || request.getDateTo() != null) && StringUtils.hasText(dateField)) {
            Criteria dateCriteria = new Criteria(dateField);

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
            // Nếu không có điều kiện nào, trả về tất cả
            return mongoTemplate.findAll(entityClass);
        }

        // Thực hiện query
        return mongoTemplate.find(query, entityClass);
    }
}

