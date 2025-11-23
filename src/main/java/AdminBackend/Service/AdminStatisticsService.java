package AdminBackend.Service;

import AdminBackend.DTO.Request.DateRangeRequest;
import AdminBackend.DTO.Response.ChartDataResponse;
import com.auctionaa.backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminStatisticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRoomRepository auctionRoomRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ReportsRepository reportsRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private BidsRepository bidsRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Thống kê số người dùng đăng ký theo ngày
     */
    public ChartDataResponse getUsersRegistrationStats(DateRangeRequest request) {
        LocalDateTime startDateTime = request.getBegin().atStartOfDay();
        LocalDateTime endDateTime = request.getEnd().atTime(23, 59, 59);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(startDateTime).lte(endDateTime)),
                Aggregation.project()
                        .andExpression("dateToString('%d/%m/%Y', createdAt)").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "users", Map.class);

        // Tạo map từ kết quả aggregation
        Map<String, Long> dataMap = results.getMappedResults().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("_id"),
                        item -> ((Number) item.get("count")).longValue()
                ));

        // Fill tất cả các ngày trong khoảng
        List<ChartDataResponse.ChartDataItem> dataItems = fillAllDatesWithCount(request.getBegin(), request.getEnd(), dataMap);

        return buildChartResponse("Thống kê người dùng đăng ký", dataItems);
    }

    /**
     * Thống kê số phòng đấu giá được lập theo ngày
     */
    public ChartDataResponse getAuctionRoomsStats(DateRangeRequest request) {
        LocalDateTime startDateTime = request.getBegin().atStartOfDay();
        LocalDateTime endDateTime = request.getEnd().atTime(23, 59, 59);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(startDateTime).lte(endDateTime)),
                Aggregation.project()
                        .andExpression("dateToString('%d/%m/%Y', createdAt)").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "auction_rooms", Map.class);

        Map<String, Long> dataMap = results.getMappedResults().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("_id"),
                        item -> ((Number) item.get("count")).longValue()
                ));

        List<ChartDataResponse.ChartDataItem> dataItems = fillAllDatesWithCount(request.getBegin(), request.getEnd(), dataMap);

        return buildChartResponse("Thống kê phòng đấu giá", dataItems);
    }

    /**
     * Thống kê doanh thu (invoice) theo ngày
     * Xử lý cả trường hợp totalAmount là String hoặc Number trong database
     */
    public ChartDataResponse getRevenueStats(DateRangeRequest request) {
        LocalDateTime startDateTime = request.getBegin().atStartOfDay();
        LocalDateTime endDateTime = request.getEnd().atTime(23, 59, 59);

        // Sử dụng $convert để chuyển String thành Double nếu cần
        // Xử lý cả trường hợp totalAmount là String hoặc Number trong database
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(startDateTime).lte(endDateTime)),
                Aggregation.project()
                        .andExpression("dateToString('%d/%m/%Y', createdAt)").as("date")
                        .andExpression(
                            "{$convert: {" +
                                "input: '$totalAmount', " +
                                "to: 'double', " +
                                "onError: 0, " +
                                "onNull: 0" +
                            "}}"
                        ).as("convertedAmount"),
                Aggregation.group("date")
                        .sum("convertedAmount").as("totalAmount"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "invoices", Map.class);

        Map<String, Double> dataMap = results.getMappedResults().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("_id"),
                        item -> {
                            Object totalAmount = item.get("totalAmount");
                            if (totalAmount instanceof BigDecimal) {
                                return ((BigDecimal) totalAmount).doubleValue();
                            } else if (totalAmount instanceof Number) {
                                return ((Number) totalAmount).doubleValue();
                            } else if (totalAmount instanceof String) {
                                try {
                                    return Double.parseDouble((String) totalAmount);
                                } catch (NumberFormatException e) {
                                    return 0.0;
                                }
                            } else {
                                return 0.0;
                            }
                        }
                ));

        List<ChartDataResponse.ChartDataItem> dataItems = fillAllDatesWithAmount(request.getBegin(), request.getEnd(), dataMap);

        return buildChartResponse("Thống kê doanh thu", dataItems, true);
    }

    /**
     * Thống kê số report được báo cáo theo ngày
     */
    public ChartDataResponse getReportsStats(DateRangeRequest request) {
        LocalDateTime startDateTime = request.getBegin().atStartOfDay();
        LocalDateTime endDateTime = request.getEnd().atTime(23, 59, 59);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(startDateTime).lte(endDateTime)),
                Aggregation.project()
                        .andExpression("dateToString('%d/%m/%Y', createdAt)").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "reports", Map.class);

        Map<String, Long> dataMap = results.getMappedResults().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("_id"),
                        item -> ((Number) item.get("count")).longValue()
                ));

        List<ChartDataResponse.ChartDataItem> dataItems = fillAllDatesWithCount(request.getBegin(), request.getEnd(), dataMap);

        return buildChartResponse("Thống kê báo cáo", dataItems);
    }

    /**
     * Thống kê số tác phẩm được thêm vào theo ngày
     */
    public ChartDataResponse getArtworksStats(DateRangeRequest request) {
        LocalDateTime startDateTime = request.getBegin().atStartOfDay();
        LocalDateTime endDateTime = request.getEnd().atTime(23, 59, 59);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(startDateTime).lte(endDateTime)),
                Aggregation.project()
                        .andExpression("dateToString('%d/%m/%Y', createdAt)").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "artworks", Map.class);

        Map<String, Long> dataMap = results.getMappedResults().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("_id"),
                        item -> ((Number) item.get("count")).longValue()
                ));

        List<ChartDataResponse.ChartDataItem> dataItems = fillAllDatesWithCount(request.getBegin(), request.getEnd(), dataMap);

        return buildChartResponse("Thống kê tác phẩm", dataItems);
    }

    /**
     * Thống kê số đấu giá (bids) theo ngày
     */
    public ChartDataResponse getBidsStats(DateRangeRequest request) {
        LocalDateTime startDateTime = request.getBegin().atStartOfDay();
        LocalDateTime endDateTime = request.getEnd().atTime(23, 59, 59);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(startDateTime).lte(endDateTime)),
                Aggregation.project()
                        .andExpression("dateToString('%d/%m/%Y', createdAt)").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "bids", Map.class);

        Map<String, Long> dataMap = results.getMappedResults().stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("_id"),
                        item -> ((Number) item.get("count")).longValue()
                ));

        List<ChartDataResponse.ChartDataItem> dataItems = fillAllDatesWithCount(request.getBegin(), request.getEnd(), dataMap);

        return buildChartResponse("Thống kê đấu giá", dataItems);
    }

    /**
     * Helper method: Fill tất cả các ngày trong khoảng từ begin đến end (cho count)
     */
    private List<ChartDataResponse.ChartDataItem> fillAllDatesWithCount(LocalDate begin, LocalDate end, Map<String, Long> countMap) {
        List<ChartDataResponse.ChartDataItem> dataItems = new ArrayList<>();
        LocalDate currentDate = begin;
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        while (!currentDate.isAfter(end)) {
            String dateStr = currentDate.format(formatter);
            ChartDataResponse.ChartDataItem dataItem = new ChartDataResponse.ChartDataItem();
            dataItem.setDate(dateStr);
            dataItem.setCount(countMap.getOrDefault(dateStr, 0L));
            dataItems.add(dataItem);
            currentDate = currentDate.plusDays(1);
        }

        return dataItems;
    }

    /**
     * Helper method: Fill tất cả các ngày trong khoảng từ begin đến end (cho revenue)
     */
    private List<ChartDataResponse.ChartDataItem> fillAllDatesWithAmount(LocalDate begin, LocalDate end, Map<String, Double> amountMap) {
        List<ChartDataResponse.ChartDataItem> dataItems = new ArrayList<>();
        LocalDate currentDate = begin;
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        while (!currentDate.isAfter(end)) {
            String dateStr = currentDate.format(formatter);
            ChartDataResponse.ChartDataItem dataItem = new ChartDataResponse.ChartDataItem();
            dataItem.setDate(dateStr);
            dataItem.setTotalAmount(amountMap.getOrDefault(dateStr, 0.0));
            dataItems.add(dataItem);
            currentDate = currentDate.plusDays(1);
        }

        return dataItems;
    }

    /**
     * Helper method: Build chart response từ data items
     */
    private ChartDataResponse buildChartResponse(String label, List<ChartDataResponse.ChartDataItem> dataItems) {
        return buildChartResponse(label, dataItems, false);
    }

    private ChartDataResponse buildChartResponse(String label, List<ChartDataResponse.ChartDataItem> dataItems, boolean isRevenue) {
        List<String> labels = dataItems.stream()
                .map(ChartDataResponse.ChartDataItem::getDate)
                .collect(Collectors.toList());

        List<Object> data = dataItems.stream()
                .map(item -> isRevenue ? (item.getTotalAmount() != null ? item.getTotalAmount() : 0.0) : item.getCount())
                .collect(Collectors.toList());

        List<String> colors = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            colors.add(String.format("#%06X", new Random().nextInt(0xFFFFFF + 1)));
        }

        ChartDataResponse.ChartDataset dataset = new ChartDataResponse.ChartDataset();
        dataset.setLabel(label);
        dataset.setData(data);
        dataset.setBackgroundColor(colors);

        ChartDataResponse response = new ChartDataResponse();
        response.setStatus(1);
        response.setMessage("Success");
        response.setData(dataItems);
        response.setLabels(labels);
        response.setDatasets(Collections.singletonList(dataset));

        return response;
    }
}

