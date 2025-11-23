package AdminBackend.Service;

import AdminBackend.DTO.Response.MonthlyComparisonResponse;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class MonthlyStatisticsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    /**
     * Tính toán thống kê so sánh tháng này vs tháng trước cho một collection
     * @param collectionName Tên collection trong MongoDB
     * @param dateField Tên field chứa ngày (ví dụ: "createdAt")
     * @return MonthlyComparisonResponse
     */
    public MonthlyComparisonResponse getMonthlyComparison(String collectionName, String dateField) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        // Tính toán tháng hiện tại
        LocalDateTime currentMonthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime currentMonthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        long currentMonthCount = countDocumentsInRange(collectionName, dateField, currentMonthStart, currentMonthEnd);

        // Tính toán tháng trước
        LocalDateTime previousMonthStart = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime previousMonthEnd = previousMonth.atEndOfMonth().atTime(23, 59, 59);
        long previousMonthCount = countDocumentsInRange(collectionName, dateField, previousMonthStart, previousMonthEnd);

        // Tính toán sự thay đổi
        long changeAmount = currentMonthCount - previousMonthCount;
        double changePercentage = previousMonthCount == 0 
            ? (currentMonthCount > 0 ? 100.0 : 0.0)
            : ((double) changeAmount / previousMonthCount) * 100.0;
        boolean isIncrease = changeAmount >= 0;

        // Tạo response
        MonthlyComparisonResponse.MonthlyComparisonData data = 
            new MonthlyComparisonResponse.MonthlyComparisonData();
        
        MonthlyComparisonResponse.MonthData currentMonthData = 
            new MonthlyComparisonResponse.MonthData(
                currentMonthCount,
                currentMonth.format(MONTH_FORMATTER)
            );
        
        MonthlyComparisonResponse.MonthData previousMonthData = 
            new MonthlyComparisonResponse.MonthData(
                previousMonthCount,
                previousMonth.format(MONTH_FORMATTER)
            );
        
        MonthlyComparisonResponse.ChangeData changeData = 
            new MonthlyComparisonResponse.ChangeData(
                changeAmount,
                Math.round(changePercentage * 100.0) / 100.0, // Làm tròn 2 chữ số
                isIncrease
            );

        data.setCurrentMonth(currentMonthData);
        data.setPreviousMonth(previousMonthData);
        data.setChange(changeData);

        return new MonthlyComparisonResponse(1, "Success", data);
    }

    /**
     * Tính toán thống kê doanh thu so sánh tháng này vs tháng trước
     * @param collectionName Tên collection (ví dụ: "invoices")
     * @param dateField Tên field chứa ngày
     * @param amountField Tên field chứa số tiền (ví dụ: "totalAmount")
     * @return MonthlyComparisonResponse với total là tổng số tiền
     */
    public MonthlyComparisonResponse getMonthlyRevenueComparison(String collectionName, String dateField, String amountField) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        // Tính toán tháng hiện tại
        LocalDateTime currentMonthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime currentMonthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        double currentMonthTotal = sumAmountInRange(collectionName, dateField, amountField, currentMonthStart, currentMonthEnd);

        // Tính toán tháng trước
        LocalDateTime previousMonthStart = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime previousMonthEnd = previousMonth.atEndOfMonth().atTime(23, 59, 59);
        double previousMonthTotal = sumAmountInRange(collectionName, dateField, amountField, previousMonthStart, previousMonthEnd);
        
        // Debug: Log để kiểm tra
        System.out.println("Revenue calculation - dateField: " + dateField + 
            ", currentMonthTotal: " + currentMonthTotal + 
            ", previousMonthTotal: " + previousMonthTotal);

        // Tính toán sự thay đổi
        long changeAmount = Math.round(currentMonthTotal - previousMonthTotal);
        double changePercentage = previousMonthTotal == 0 
            ? (currentMonthTotal > 0 ? 100.0 : 0.0)
            : ((currentMonthTotal - previousMonthTotal) / previousMonthTotal) * 100.0;
        boolean isIncrease = changeAmount >= 0;

        // Tạo response
        MonthlyComparisonResponse.MonthlyComparisonData data = 
            new MonthlyComparisonResponse.MonthlyComparisonData();
        
        MonthlyComparisonResponse.MonthData currentMonthData = 
            new MonthlyComparisonResponse.MonthData(
                Math.round(currentMonthTotal),
                currentMonth.format(MONTH_FORMATTER)
            );
        
        MonthlyComparisonResponse.MonthData previousMonthData = 
            new MonthlyComparisonResponse.MonthData(
                Math.round(previousMonthTotal),
                previousMonth.format(MONTH_FORMATTER)
            );
        
        MonthlyComparisonResponse.ChangeData changeData = 
            new MonthlyComparisonResponse.ChangeData(
                changeAmount,
                Math.round(changePercentage * 100.0) / 100.0,
                isIncrease
            );

        data.setCurrentMonth(currentMonthData);
        data.setPreviousMonth(previousMonthData);
        data.setChange(changeData);

        return new MonthlyComparisonResponse(1, "Success", data);
    }

    /**
     * Đếm số documents trong một khoảng thời gian
     */
    private long countDocumentsInRange(String collectionName, String dateField, 
                                       LocalDateTime start, LocalDateTime end) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where(dateField).gte(start).lte(end)),
            Aggregation.count().as("count")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, collectionName, Map.class);

        if (results.getMappedResults().isEmpty()) {
            return 0L;
        }

        Map<String, Object> result = results.getMappedResults().get(0);
        Object count = result.get("count");
        if (count instanceof Number) {
            return ((Number) count).longValue();
        }
        return 0L;
    }

    /**
     * Tính tổng số tiền trong một khoảng thời gian
     * Đối với invoices, tính tất cả invoice có totalAmount không null
     * Nếu dateField là "orderDate", dùng $or để query cả orderDate và createdAt
     */
    private double sumAmountInRange(String collectionName, String dateField, String amountField,
                                   LocalDateTime start, LocalDateTime end) {
        // Nếu là invoices và dateField là orderDate, dùng $or để query cả orderDate và createdAt
        if ("invoices".equals(collectionName) && "orderDate".equals(dateField)) {
            // Dùng $or: orderDate trong khoảng HOẶC (orderDate không tồn tại/null VÀ createdAt trong khoảng)
            Criteria criteria = new Criteria().orOperator(
                Criteria.where("orderDate").gte(start).lte(end),
                new Criteria().andOperator(
                    new Criteria().orOperator(
                        Criteria.where("orderDate").is(null),
                        Criteria.where("orderDate").exists(false)
                    ),
                    Criteria.where("createdAt").gte(start).lte(end)
                )
            ).and(amountField).ne(null);
            
            return sumAmountWithCriteria(collectionName, criteria, amountField);
        } else {
            // Các trường hợp khác: chỉ filter theo dateField
            Criteria criteria = Criteria.where(dateField).gte(start).lte(end)
                .and(amountField).ne(null);
            return sumAmountWithCriteria(collectionName, criteria, amountField);
        }
    }
    
    /**
     * Helper method: Tính tổng với criteria cho trước
     * Xử lý Decimal128 từ MongoDB (BigDecimal được lưu dưới dạng Decimal128)
     * Xử lý cả trường hợp totalAmount là String trong database
     */
    private double sumAmountWithCriteria(String collectionName, Criteria criteria, String amountField) {
        // Debug: Log criteria (không dùng toJson() vì có thể chứa LocalDateTime)
        System.out.println("DEBUG sumAmountWithCriteria - collection: " + collectionName + 
            ", amountField: " + amountField);
        
        // Sử dụng $convert để chuyển String thành Double nếu cần
        // Xử lý cả trường hợp totalAmount là String hoặc Number trong database
        // Sử dụng $convert với onError để xử lý lỗi an toàn
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(criteria),
            Aggregation.project()
                .andExpression(
                    "{$convert: {" +
                        "input: '$" + amountField + "', " +
                        "to: 'double', " +
                        "onError: 0, " +
                        "onNull: 0" +
                    "}}"
                ).as("convertedAmount"),
            Aggregation.group().sum("convertedAmount").as("total")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, collectionName, Map.class);

        // Debug: Log số documents matched
        long matchedCount = mongoTemplate.count(
            org.springframework.data.mongodb.core.query.Query.query(criteria), 
            collectionName
        );
        System.out.println("DEBUG - Matched documents count: " + matchedCount);

        if (results.getMappedResults().isEmpty()) {
            System.out.println("DEBUG - No aggregation results");
            return 0.0;
        }

        Map<String, Object> result = results.getMappedResults().get(0);
        Object total = result.get("total");
        System.out.println("DEBUG - Total value type: " + (total != null ? total.getClass().getName() : "null") + 
            ", value: " + total);
        
        if (total == null) {
            return 0.0;
        }
        
        // Xử lý Decimal128 (MongoDB lưu BigDecimal dưới dạng Decimal128)
        if (total instanceof Decimal128) {
            double value = ((Decimal128) total).bigDecimalValue().doubleValue();
            System.out.println("DEBUG - Converted Decimal128 to double: " + value);
            return value;
        }
        if (total instanceof BigDecimal) {
            return ((BigDecimal) total).doubleValue();
        }
        if (total instanceof Number) {
            return ((Number) total).doubleValue();
        }
        // Nếu là String (trường hợp MongoDB trả về string), parse
        if (total instanceof String) {
            try {
                return Double.parseDouble((String) total);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}

