package AdminBackend.Service;

import AdminBackend.DTO.Response.DashboardOverviewResponse;
import AdminBackend.DTO.Response.DashboardStatisticsResponse;
import AdminBackend.DTO.Response.MonthlyComparisonResponse;
import com.auctionaa.backend.Entity.*;
import com.auctionaa.backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private BidsRepository bidsRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AuctionRoomRepository auctionRoomRepository;

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    @Autowired
    private MonthlyStatisticsService monthlyStatisticsService;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Lấy thống kê chung cho dashboard
     */
    public DashboardStatisticsResponse getDashboardStatistics() {
        DashboardStatisticsResponse.StatisticsData data = new DashboardStatisticsResponse.StatisticsData();

        // Thống kê có so sánh tháng
        data.setTotalUsers(getUsersMonthlyStat());
        data.setTotalArtworks(getArtworksMonthlyStat());
        data.setTotalBids(getBidsMonthlyStat());
        data.setRevenue(getRevenueMonthlyStat());

        // Thống kê đơn giản
        data.setTotalAuctionRooms(auctionRoomRepository.count());
        data.setActiveUsers(userRepository.countByStatus(1)); // status = 1 là active

        return new DashboardStatisticsResponse(1, "Success", data);
    }

    /**
     * Thống kê users so sánh tháng
     */
    private DashboardStatisticsResponse.MonthlyStat getUsersMonthlyStat() {
        MonthlyComparisonResponse comparison = monthlyStatisticsService.getMonthlyComparison("users", "createdAt");
        MonthlyComparisonResponse.MonthlyComparisonData compData = comparison.getData();
        
        long totalUsers = userRepository.count();
        
        return new DashboardStatisticsResponse.MonthlyStat(
            totalUsers,
            compData.getCurrentMonth().getTotal(),
            compData.getPreviousMonth().getTotal(),
            compData.getChange().getAmount(),
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease()
        );
    }

    /**
     * Thống kê artworks so sánh tháng
     */
    private DashboardStatisticsResponse.MonthlyStat getArtworksMonthlyStat() {
        MonthlyComparisonResponse comparison = monthlyStatisticsService.getMonthlyComparison("artworks", "createdAt");
        MonthlyComparisonResponse.MonthlyComparisonData compData = comparison.getData();
        
        long totalArtworks = artworkRepository.count();
        
        return new DashboardStatisticsResponse.MonthlyStat(
            totalArtworks,
            compData.getCurrentMonth().getTotal(),
            compData.getPreviousMonth().getTotal(),
            compData.getChange().getAmount(),
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease()
        );
    }

    /**
     * Thống kê bids so sánh tháng
     */
    private DashboardStatisticsResponse.MonthlyStat getBidsMonthlyStat() {
        MonthlyComparisonResponse comparison = monthlyStatisticsService.getMonthlyComparison("bids", "createdAt");
        MonthlyComparisonResponse.MonthlyComparisonData compData = comparison.getData();
        
        long totalBids = bidsRepository.count();
        
        return new DashboardStatisticsResponse.MonthlyStat(
            totalBids,
            compData.getCurrentMonth().getTotal(),
            compData.getPreviousMonth().getTotal(),
            compData.getChange().getAmount(),
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease()
        );
    }

    /**
     * Thống kê revenue so sánh tháng
     * Tính tổng totalAmount từ tất cả Invoice (không filter theo status)
     * Dùng createdAt để tính revenue (giống các API thống kê khác)
     */
    private DashboardStatisticsResponse.MonthlyRevenueStat getRevenueMonthlyStat() {
        // Dùng createdAt để tính revenue (đảm bảo consistency với các API thống kê khác)
        MonthlyComparisonResponse comparison = monthlyStatisticsService.getMonthlyRevenueComparison("invoices", "createdAt", "totalAmount");
        MonthlyComparisonResponse.MonthlyComparisonData compData = comparison.getData();
        
        BigDecimal currentMonth = BigDecimal.valueOf(compData.getCurrentMonth().getTotal());
        BigDecimal previousMonth = BigDecimal.valueOf(compData.getPreviousMonth().getTotal());
        BigDecimal change = BigDecimal.valueOf(compData.getChange().getAmount());
        
        // Tính tổng doanh thu từ tất cả invoices
        BigDecimal totalRevenue = calculateTotalRevenue();
        
        System.out.println("DEBUG getRevenueMonthlyStat - currentMonth: " + currentMonth + 
            ", previousMonth: " + previousMonth + 
            ", change: " + change +
            ", totalRevenue: " + totalRevenue);
        
        return new DashboardStatisticsResponse.MonthlyRevenueStat(
            totalRevenue,
            currentMonth,
            previousMonth,
            change,
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease()
        );
    }
    
    /**
     * Tính tổng doanh thu từ tất cả invoices (không filter theo thời gian)
     */
    private BigDecimal calculateTotalRevenue() {
        org.springframework.data.mongodb.core.aggregation.Aggregation aggregation = 
            org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                    org.springframework.data.mongodb.core.query.Criteria.where("totalAmount").ne(null)
                ),
                org.springframework.data.mongodb.core.aggregation.Aggregation.project()
                    .andExpression(
                        "{$convert: {" +
                            "input: '$totalAmount', " +
                            "to: 'double', " +
                            "onError: 0, " +
                            "onNull: 0" +
                        "}}"
                    ).as("convertedAmount"),
                org.springframework.data.mongodb.core.aggregation.Aggregation.group()
                    .sum("convertedAmount").as("total")
            );

        org.springframework.data.mongodb.core.aggregation.AggregationResults<java.util.Map> results = 
            mongoTemplate.aggregate(aggregation, "invoices", java.util.Map.class);

        if (results.getMappedResults().isEmpty()) {
            return BigDecimal.ZERO;
        }

        java.util.Map<String, Object> result = results.getMappedResults().get(0);
        Object total = result.get("total");
        
        if (total == null) {
            return BigDecimal.ZERO;
        }
        
        if (total instanceof Number) {
            return BigDecimal.valueOf(((Number) total).doubleValue());
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Top 10 AuctionRoom mới nhất (theo startedAt) với sessions
     */
    public List<DashboardOverviewResponse.AuctionRoomWithSessions> getTop10AuctionRooms() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startedAt"));
        List<AuctionRoom> rooms = auctionRoomRepository.findAll(pageable).getContent();

        return rooms.stream().map(room -> {
            DashboardOverviewResponse.AuctionRoomWithSessions roomWithSessions = 
                new DashboardOverviewResponse.AuctionRoomWithSessions();
            roomWithSessions.setId(room.getId());
            roomWithSessions.setRoomName(room.getRoomName());
            roomWithSessions.setDescription(room.getDescription());
            roomWithSessions.setImageAuctionRoom(room.getImageAuctionRoom());
            roomWithSessions.setType(room.getType());
            roomWithSessions.setStatus(room.getStatus());
            roomWithSessions.setStartedAt(room.getStartedAt());
            roomWithSessions.setStoppedAt(room.getStoppedAt());

            // Lấy tất cả sessions của room này
            List<AuctionSession> sessions = auctionSessionRepository.findByAuctionRoomId(room.getId());
            List<DashboardOverviewResponse.SessionInfo> sessionInfos = sessions.stream()
                .map(this::mapToSessionInfo)
                .collect(Collectors.toList());
            roomWithSessions.setSessions(sessionInfos);

            return roomWithSessions;
        }).collect(Collectors.toList());
    }

    /**
     * Map AuctionSession sang SessionInfo
     */
    private DashboardOverviewResponse.SessionInfo mapToSessionInfo(AuctionSession session) {
        DashboardOverviewResponse.SessionInfo info = new DashboardOverviewResponse.SessionInfo();
        info.setId(session.getId());
        info.setArtworkId(session.getArtworkId());
        info.setImageUrl(session.getImageUrl());
        info.setStartingPrice(session.getStartingPrice());
        info.setCurrentPrice(session.getCurrentPrice());
        info.setStatus(session.getStatus());
        info.setOrderIndex(session.getOrderIndex());
        info.setStartTime(session.getStartTime());
        info.setEndedAt(session.getEndedAt());

        // Lấy artwork title nếu có
        if (session.getArtworkId() != null) {
            artworkRepository.findById(session.getArtworkId()).ifPresent(artwork -> {
                info.setArtworkTitle(artwork.getTitle());
            });
        }

        return info;
    }

    /**
     * Top 10 User mới đăng ký nhất (theo createdAt)
     */
    public List<DashboardOverviewResponse.NewUserInfo> getTop10NewUsers() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<User> users = userRepository.findAll(pageable).getContent();

        return users.stream().map(user -> {
            DashboardOverviewResponse.NewUserInfo info = new DashboardOverviewResponse.NewUserInfo();
            info.setId(user.getId());
            info.setUsername(user.getUsername());
            info.setEmail(user.getEmail());
            info.setAvt(user.getAvt());
            info.setCreatedAt(user.getCreatedAt());
            info.setStatus(user.getStatus());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * Top 10 Session có giá đấu giá cao nhất (theo currentPrice từ AuctionSession)
     * Lấy từ AuctionSession để đảm bảo dữ liệu chính xác
     */
    public List<DashboardOverviewResponse.TopSessionInfo> getTop10SessionsByPrice() {
        // Lấy tất cả sessions và sort trong memory để đảm bảo chính xác
        List<AuctionSession> allSessions = auctionSessionRepository.findAll();
        
        // Filter sessions: status = 0 (đã đấu giá xong) và currentPrice > 0, lấy top 10
        List<AuctionSession> sessions = allSessions.stream()
            .filter(session -> session.getStatus() == 0 // Chỉ lấy sessions đã đấu giá xong
                && session.getCurrentPrice() != null 
                && session.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0)
            .sorted((s1, s2) -> s2.getCurrentPrice().compareTo(s1.getCurrentPrice())) // Sort DESC
            .limit(10)
            .collect(Collectors.toList());
        
        // Debug: Log số lượng sessions
        System.out.println("DEBUG getTop10SessionsByPrice - Total sessions: " + allSessions.size() + 
            ", Filtered sessions: " + sessions.size());
        
        if (sessions.isEmpty()) {
            return List.of();
        }
        
        // Load tất cả dữ liệu liên quan một lần để tránh N+1 queries
        List<String> sessionIds = sessions.stream().map(AuctionSession::getId).collect(Collectors.toList());
        List<String> artworkIds = sessions.stream()
            .map(AuctionSession::getArtworkId)
            .filter(id -> id != null && !id.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        List<String> roomIds = sessions.stream()
            .map(AuctionSession::getAuctionRoomId)
            .filter(id -> id != null && !id.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        List<String> userIds = sessions.stream()
            .map(AuctionSession::getWinnerId)
            .filter(id -> id != null && !id.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        
        // Load Invoice theo sessionIds
        Query invoiceQuery = new Query(Criteria.where("sessionId").in(sessionIds));
        List<Invoice> invoices = mongoTemplate.find(invoiceQuery, Invoice.class);
        java.util.Map<String, Invoice> invoiceMap = invoices.stream()
            .collect(Collectors.toMap(Invoice::getSessionId, inv -> inv, (first, second) -> first));
        
        // Thêm userId từ invoice vào danh sách userIds để load user info
        List<String> invoiceUserIds = invoices.stream()
            .map(Invoice::getUserId)
            .filter(id -> id != null && !id.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        userIds.addAll(invoiceUserIds);
        
        // Load Artworks
        java.util.Map<String, Artwork> artworkMap = artworkIds.isEmpty() ? 
            java.util.Collections.emptyMap() :
            artworkRepository.findAllById(artworkIds).stream()
                .collect(Collectors.toMap(Artwork::getId, a -> a));
        
        // Load Users (artists và winners)
        List<String> artistIds = artworkMap.values().stream()
            .map(Artwork::getOwnerId)
            .filter(id -> id != null && !id.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        userIds.addAll(artistIds);
        userIds = userIds.stream().distinct().collect(Collectors.toList());
        
        java.util.Map<String, User> userMap = userIds.isEmpty() ?
            java.util.Collections.emptyMap() :
            userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        // Load AuctionRooms
        java.util.Map<String, AuctionRoom> roomMap = roomIds.isEmpty() ?
            java.util.Collections.emptyMap() :
            auctionRoomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(AuctionRoom::getId, r -> r));
        
        // Map sessions sang TopSessionInfo
        return sessions.stream().map(session -> {
            DashboardOverviewResponse.TopSessionInfo info = new DashboardOverviewResponse.TopSessionInfo();
            
            // Lấy thông tin chính từ AuctionSession
            info.setSessionId(session.getId());
            info.setAuctionRoomId(session.getAuctionRoomId());
            info.setArtworkId(session.getArtworkId());
            info.setViewCount(session.getViewCount() != null ? session.getViewCount() : 0);
            
            // Lấy thông tin từ Artwork
            Artwork artwork = artworkMap.get(session.getArtworkId());
            if (artwork != null) {
                info.setArtworkTitle(artwork.getTitle());
                
                // Lấy artworkImageUrl: ưu tiên session.imageUrl -> artwork.avtArtwork -> artwork.imageUrls[0]
                if (session.getImageUrl() != null && !session.getImageUrl().isEmpty()) {
                    info.setArtworkImageUrl(session.getImageUrl());
                } else if (artwork.getAvtArtwork() != null && !artwork.getAvtArtwork().isEmpty()) {
                    info.setArtworkImageUrl(artwork.getAvtArtwork());
                } else if (artwork.getImageUrls() != null && !artwork.getImageUrls().isEmpty()) {
                    info.setArtworkImageUrl(artwork.getImageUrls().get(0));
                }
                
                info.setPaintingGenre(artwork.getPaintingGenre()); // Lấy thể loại tranh
                
                // Lấy artist name
                if (artwork.getOwnerId() != null) {
                    User artist = userMap.get(artwork.getOwnerId());
                    if (artist != null) {
                        info.setArtistName(artist.getUsername());
                    }
                }
            }
            
            // Set default nếu không có artwork
            if (info.getArtworkTitle() == null) {
                info.setArtworkTitle("Unknown Artwork");
            }
            if (info.getArtistName() == null) {
                info.setArtistName("Unknown Artist");
            }
            
            // Lấy thông tin từ AuctionRoom
            AuctionRoom room = roomMap.get(session.getAuctionRoomId());
            if (room != null) {
                info.setRoomName(room.getRoomName());
            } else {
                info.setRoomName("Unknown Room");
            }
            
            // totalAmount luôn lấy từ current_price của AuctionSession
            info.setTotalAmount(session.getCurrentPrice());
            
            // Lấy thông tin từ Invoice nếu có (winner, orderDate)
            Invoice invoice = invoiceMap.get(session.getId());
            if (invoice != null) {
                info.setOrderDate(invoice.getOrderDate());
                
                // Lấy winnerName và winnerEmail từ invoice, nếu không có thì lấy từ invoice.userId
                if (invoice.getWinnerName() != null && !invoice.getWinnerName().isEmpty()) {
                    info.setWinnerName(invoice.getWinnerName());
                } else if (invoice.getUserId() != null) {
                    User winner = userMap.get(invoice.getUserId());
                    if (winner != null) {
                        info.setWinnerName(winner.getUsername());
                    }
                }
                
                if (invoice.getWinnerEmail() != null && !invoice.getWinnerEmail().isEmpty()) {
                    info.setWinnerEmail(invoice.getWinnerEmail());
                } else if (invoice.getUserId() != null) {
                    User winner = userMap.get(invoice.getUserId());
                    if (winner != null) {
                        info.setWinnerEmail(winner.getEmail());
                    }
                }
                
                // Nếu vẫn không có, thử lấy từ session.winnerId
                if ((info.getWinnerName() == null || info.getWinnerName().isEmpty()) 
                    && session.getWinnerId() != null) {
                    User winner = userMap.get(session.getWinnerId());
                    if (winner != null) {
                        info.setWinnerName(winner.getUsername());
                        info.setWinnerEmail(winner.getEmail());
                    }
                }
            } else {
                // Nếu không có Invoice, lấy winner từ session nếu có
                if (session.getWinnerId() != null) {
                    User winner = userMap.get(session.getWinnerId());
                    if (winner != null) {
                        info.setWinnerName(winner.getUsername());
                        info.setWinnerEmail(winner.getEmail());
                    }
                }
            }
            
            return info;
        }).collect(Collectors.toList());
    }
}

