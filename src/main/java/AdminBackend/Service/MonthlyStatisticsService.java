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
        double changePercentage;
        boolean isIncrease;
        
        if (previousMonthCount == 0) {
            // Trường hợp tháng trước = 0
            if (currentMonthCount > 0) {
                // Tháng này có dữ liệu, tháng trước không có -> tăng 100%
                changePercentage = 100.0;
                isIncrease = true;
            } else {
                // Cả hai tháng đều = 0 -> không thay đổi
                changePercentage = 0.0;
                isIncrease = false; // Không phải tăng, cũng không phải giảm
            }
        } else {
            // Trường hợp tháng trước > 0
            // Tính phần trăm thay đổi (luôn là số dương - giá trị tuyệt đối)
            changePercentage = Math.abs(((double) changeAmount / previousMonthCount) * 100.0);
            isIncrease = changeAmount > 0; // true nếu tăng, false nếu giảm hoặc không đổi
        }

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
        double changePercentage;
        boolean isIncrease;
        
        if (previousMonthTotal == 0) {
            // Trường hợp tháng trước = 0
            if (currentMonthTotal > 0) {
                // Tháng này có doanh thu, tháng trước không có -> tăng 100%
                changePercentage = 100.0;
                isIncrease = true;
            } else {
                // Cả hai tháng đều = 0 -> không thay đổi
                changePercentage = 0.0;
                isIncrease = false; // Không phải tăng, cũng không phải giảm
            }
        } else {
            // Trường hợp tháng trước > 0
            // Tính phần trăm thay đổi (luôn là số dương - giá trị tuyệt đối)
            changePercentage = Math.abs(((currentMonthTotal - previousMonthTotal) / previousMonthTotal) * 100.0);
            isIncrease = (currentMonthTotal - previousMonthTotal) > 0;
        }

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
     * ✅ Đối với invoices: CHỈ tính từ invoices đã thanh toán (paymentStatus = 1)
     * ✅ Lấy từ trường totalAmount (không phải amount)
     * Nếu dateField là "orderDate", dùng $or để query cả orderDate và createdAt
     */
    private double sumAmountInRange(String collectionName, String dateField, String amountField,
                                   LocalDateTime start, LocalDateTime end) {
        Criteria criteria;
        
        // ✅ Đối với invoices: CHỈ tính từ invoices đã thanh toán (paymentStatus = 1)
        // ✅ Lấy từ trường totalAmount (không phải amount) - amountField đã được truyền vào là "totalAmount"
        if ("invoices".equals(collectionName)) {
            // Base criteria: paymentStatus = 1 (đã thanh toán) và totalAmount không null
            criteria = Criteria.where("paymentStatus").is(1)
                .and(amountField).ne(null);
            
            // Nếu dateField là "orderDate", dùng $or để query cả orderDate và createdAt
            if ("orderDate".equals(dateField)) {
                criteria = criteria.andOperator(
                    new Criteria().orOperator(
                        Criteria.where("orderDate").gte(start).lte(end),
                        new Criteria().andOperator(
                            new Criteria().orOperator(
                                Criteria.where("orderDate").is(null),
                                Criteria.where("orderDate").exists(false)
                            ),
                            Criteria.where("createdAt").gte(start).lte(end)
                        )
                    )
                );
            } else {
                // Các dateField khác (như "createdAt"): chỉ filter theo dateField
                criteria = criteria.and(dateField).gte(start).lte(end);
            }
            
            // Debug: Log criteria cho invoices
            System.out.println("DEBUG sumAmountInRange - invoices: dateField=" + dateField + 
                ", start=" + start + ", end=" + end);
        } else {
            // Các collection khác: chỉ filter theo dateField
            criteria = Criteria.where(dateField).gte(start).lte(end)
                .and(amountField).ne(null);
        }
        
        return sumAmountWithCriteria(collectionName, criteria, amountField);
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
        
        // ✅ Luôn dùng $convert để xử lý cả trường hợp totalAmount là String hoặc Number
        // Đảm bảo hoạt động đúng với mọi trường hợp
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
        
        // Xử lý Decimal128 (MongoDB lưu BigDecimal dưới dạng Decimal128) - giữ precision
        if (total instanceof Decimal128) {
            BigDecimal bigDecimalValue = ((Decimal128) total).bigDecimalValue();
            double value = bigDecimalValue.doubleValue();
            System.out.println("DEBUG - Converted Decimal128 to double: " + value + " (BigDecimal: " + bigDecimalValue + ")");
            return value;
        }
        if (total instanceof BigDecimal) {
            double value = ((BigDecimal) total).doubleValue();
            System.out.println("DEBUG - Converted BigDecimal to double: " + value + " (BigDecimal: " + total + ")");
            return value;
        }
        if (total instanceof Number) {
            double value = ((Number) total).doubleValue();
            System.out.println("DEBUG - Number to double: " + value);
            return value;
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

