package com.auctionaa.backend.Service;

import com.auctionaa.backend.Config.MbProps;
import com.auctionaa.backend.DTO.Request.AuctionSessionCreateRequest;
import com.auctionaa.backend.DTO.Request.StreamStartRequest;
import com.auctionaa.backend.Entity.*;
import com.auctionaa.backend.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StreamService {
    private final AuctionRoomRepository auctionRoomRepository;
    private final CloudinaryService cloudinaryService;
    private final BidsRepository bidsRepository;
    private final ArtworkRepository artworkRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuctionSessionRepository auctionSessionRepository;
    private final NotificationService notificationService;
    private final MbProps mbProps;
    private final AuctionRoomService auctionRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int DEFAULT_SESSION_DURATION_MINUTES = 10;
    private static final int DEFAULT_MAX_EXTENSION_BUFFER_SECONDS = 600;
    private static final int DEFAULT_EXTEND_STEP_SECONDS = 120;
    private static final int DEFAULT_EXTEND_THRESHOLD_SECONDS = 60;
    private static final String SESSION_TOPIC_PREFIX = "/topic/auction-room/";

    public AuctionRoom startStream(String roomId) {
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        
        // Set status = 1 (Đang diễn ra)
        room.setStatus(1);
        
        // Set startedAt nếu chưa có (trường hợp tạo phòng không có startedAt)
        if (room.getStartedAt() == null) {
            room.setStartedAt(LocalDateTime.now());
        }
        
        room.setUpdatedAt(LocalDateTime.now());
        return auctionRoomRepository.save(room);
    }

    public AuctionRoom createdStream(StreamStartRequest rq, MultipartFile file) throws IOException {
        // Tạo phòng
        AuctionRoom room = new AuctionRoom();
        room.generateId();
        room.setAdminId(rq.getAdminId());
        room.setRoomName(rq.getRoomName());
        
        // Set startedAt: từ request nếu có, nếu không thì dùng now
        if (rq.getStartedAt() != null) {
            room.setStartedAt(rq.getStartedAt());
        } else {
            room.setStartedAt(LocalDateTime.now());
        }
        
        room.setDescription(rq.getDescription());
        room.setType(rq.getType() != null ? rq.getType() : "public");
        room.setMemberIds(new ArrayList<>(List.of(rq.getAdminId())));
        room.setStatus(2); // CREATED
        room.setViewCount(0);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        // Set imageAuctionRoom từ request (URL string) hoặc upload file
        if (rq.getImageAuctionRoom() != null && !rq.getImageAuctionRoom().isBlank()) {
            room.setImageAuctionRoom(rq.getImageAuctionRoom());
        } else if (file != null && !file.isEmpty()) {
            CloudinaryService.UploadResult result = cloudinaryService.uploadImage(file,
                    "auctionaa/liveStream/" + room.getId(), "cover", null);
            room.setImageAuctionRoom(result.getUrl());
        }

        // Set estimatedEndTime từ request
        if (rq.getEstimatedEndTime() != null) {
            room.setEstimatedEndTime(rq.getEstimatedEndTime());
        }

        // Set paymentDeadlineDays từ request
        if (rq.getPaymentDeadlineDays() != null) {
            room.setPaymentDeadlineDays(rq.getPaymentDeadlineDays());
        }

        if (room.getRoomName() == null || room.getRoomName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name is required");
        }

        AuctionRoom savedRoom = auctionRoomRepository.save(room);
        System.out.println("✅ Saved room ID: " + savedRoom.getId());

        int i = 0;
        // 2️⃣ Tạo danh sách Session
        if (rq.getSessions() != null && !rq.getSessions().isEmpty()) {
            List<AuctionSession> sessions = new ArrayList<>();
            for (AuctionSessionCreateRequest s : rq.getSessions()) {
                // Validate session
                if (s.getArtworkId() == null || s.getArtworkId().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "artworkId is required for each session");
                }
                if (s.getStartingPrice() == null || s.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startingPrice must be > 0 for artwork: " + s.getArtworkId());
                }
                if (s.getBidStep() == null || s.getBidStep().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bidStep must be > 0 for artwork: " + s.getArtworkId());
                }

                AuctionSession session = new AuctionSession();
                session.generateId();
                session.setAuctionRoomId(savedRoom.getId());
                session.setArtworkId(s.getArtworkId());
                session.setImageUrl(s.getImageUrl());
                session.setStartingPrice(s.getStartingPrice());
                session.setCurrentPrice(s.getStartingPrice());
                session.setBidStep(s.getBidStep());
                session.setStatus(0); // STOPPED/DRAFT
                session.setOrderIndex(i++);
                session.setStartTime(null); // mặc định null, sẽ set khi start
                session.setEndedAt(null);
                session.setCreatedAt(LocalDateTime.now());
                session.setUpdatedAt(LocalDateTime.now());
                session.setBidCount(0);
                session.setViewCount(0);

                int durationMinutes = (s.getDurationMinutes() > 0) 
                        ? s.getDurationMinutes() 
                        : DEFAULT_SESSION_DURATION_MINUTES;
                int durationSeconds = durationMinutes * 60;
                session.setDurationSeconds(durationSeconds);
                session.setDurationMinutes(durationMinutes);
                session.setMaxDurationSeconds(resolveMaxDurationSeconds(s, durationSeconds));
                session.setExtendStepSeconds(DEFAULT_EXTEND_STEP_SECONDS);
                session.setExtendThresholdSeconds(DEFAULT_EXTEND_THRESHOLD_SECONDS);

                sessions.add(session);
            }
            auctionSessionRepository.saveAll(sessions);
        }

        // Set depositAmount: từ request nếu có, nếu không thì tính toán
        if (rq.getDepositAmount() != null) {
            savedRoom.setDepositAmount(rq.getDepositAmount());
        } else {
            BigDecimal depositAmount = auctionRoomService.recomputeAndPersistDeposit(savedRoom.getId());
            savedRoom.setDepositAmount(depositAmount);
        }
        
        // Save lại nếu có thay đổi depositAmount
        savedRoom = auctionRoomRepository.save(savedRoom);

        return savedRoom;
    }

    public Optional<AuctionRoom> getRoom(String roomId) {
        Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(roomId);
        roomOpt.ifPresent(room -> {
            room.setViewCount((room.getViewCount() == null ? 0 : room.getViewCount()) + 1);
            BigDecimal depositAmount = auctionRoomService.recomputeAndPersistDeposit(room.getId());
            room.setDepositAmount(depositAmount);
            auctionRoomRepository.save(room);
        });
        return roomOpt;
    }

    @Transactional
    public List<Invoice> stopStreamAndGenerateInvoice(String roomId) {
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        room.setStoppedAt(LocalDateTime.now());
        room.setStatus(0);
        auctionRoomRepository.save(room);

        List<AuctionSession> sessions = auctionSessionRepository.findByAuctionRoomId(roomId);
        if (sessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No sessions found for this room");
        }

        List<Invoice> invoices = new ArrayList<>();
        List<Notification> notifications = new ArrayList<>();

        for (AuctionSession session : sessions) {
            Artwork artwork = artworkRepository.findById(session.getArtworkId()).orElse(null);
            var topBidOpt = bidsRepository.findTopByAuctionSessionIdOrderByAmountAtThatTimeDesc(session.getId());
            if (topBidOpt.isEmpty())
                continue;

            Bids topBid = topBidOpt.get();
            User winner = userRepository.findById(topBid.getUserId()).orElse(null);
            if (winner == null)
                continue;

            User artist = (artwork != null && artwork.getOwnerId() != null)
                    ? userRepository.findById(artwork.getOwnerId()).orElse(null)
                    : null;

            BigDecimal artworkPrice = topBid.getAmountAtThatTime();
            BigDecimal buyerPremium = artworkPrice.multiply(BigDecimal.valueOf(0.15));
            BigDecimal insurance = BigDecimal.valueOf(125);
            BigDecimal shipping = BigDecimal.valueOf(75);
            BigDecimal salesTax = artworkPrice.multiply(BigDecimal.valueOf(0.085));
            BigDecimal total = artworkPrice.add(buyerPremium).add(insurance).add(shipping).add(salesTax);
            
            // Làm tròn xuống (floor) tất cả các giá trị tiền về số nguyên (chỉ lấy phần trước dấu phẩy)
            RoundingMode roundingMode = RoundingMode.FLOOR;
            artworkPrice = artworkPrice.setScale(0, roundingMode);
            buyerPremium = buyerPremium.setScale(0, roundingMode);
            insurance = insurance.setScale(0, roundingMode);
            shipping = shipping.setScale(0, roundingMode);
            salesTax = salesTax.setScale(0, roundingMode);
            total = total.setScale(0, roundingMode);

            Invoice invoice = new Invoice();
            invoice.generateId();
            invoice.setAuctionRoomId(roomId);
            invoice.setSessionId(session.getId());
            invoice.setUserId(winner.getId());
            invoice.setArtworkId(artwork != null ? artwork.getId() : null);
            invoice.setArtworkTitle(artwork != null ? artwork.getTitle() : "Unknown Artwork");
            invoice.setArtistName(artist != null ? artist.getUsername() : "Unknown Artist");
            invoice.setRoomName(room.getRoomName());
            invoice.setWinnerName(winner.getUsername());
            invoice.setWinnerEmail(winner.getEmail());
            String imageUrl = session.getImageUrl() != null ? session.getImageUrl()
                    : (artwork != null ? artwork.getAvtArtwork() : null);
            invoice.setArtworkImageUrl(imageUrl);
            invoice.setAmount(artworkPrice);
            invoice.setBuyerPremium(buyerPremium);
            invoice.setInsuranceFee(insurance);
            invoice.setSalesTax(salesTax);
            invoice.setShippingFee(shipping);
            invoice.setTotalAmount(total);
            invoice.setPaymentStatus(0);
            invoice.setOrderDate(LocalDateTime.now());
            invoice.setCreatedAt(LocalDateTime.now());
            invoice.setUpdatedAt(LocalDateTime.now());

            // 🔹 Tạo note riêng cho thanh toán invoice (để tra soát MB Bank)
            String paymentNote = "INV-" + invoice.getId(); // ví dụ: INV-INV202511130001

            // 🔹 Tạo QR VietQR cho việc thanh toán invoice này
            String paymentQr = String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s%s",
                    url(mbProps.getBankCode()),
                    url(mbProps.getAccountNo()),
                    url(total.toPlainString()), // tổng tiền phải thanh toán
                    url(paymentNote), // nội dung chuyển khoản để đối soát
                    (mbProps.getAccountName() != null && !mbProps.getAccountName().isBlank())
                            ? "&accountName=" + url(mbProps.getAccountName())
                            : "");

            // 🔹 Lưu QR vào invoice
            invoice.setPaymentQr(paymentQr);

            invoices.add(invoice);

            // === Notification cho winner ===
            Notification noti = new Notification();
            noti.generateId();
            noti.setUserId(winner.getId());
            noti.setTitle("Bạn đã thắng phiên đấu giá");
            noti.setNotificationType(1);
            noti.setNotificationContent(String.format(
                    "Chúc mừng! Bạn thắng phiên %s với tác phẩm \"%s\". Tổng thanh toán: %s.",
                    session.getId(),
                    invoice.getArtworkTitle(),
                    total.toPlainString()));
            // Nếu cần deep link:
            // noti.setLink("/invoices/" + invoice.getId());
            notifications.add(noti);
        }

        invoiceRepository.saveAll(invoices);
        if (!notifications.isEmpty())
            notificationService.addAll(notifications);

        return invoices;
    }

    public AuctionSession startNextSession(String roomId) {
        AuctionSession started = startNextSessionInternal(roomId);
        publishSessionEvent(started, "SESSION_STARTED", Map.of("autoStart", false));
        return started;
    }

    public Map<String, Object> stopAuctionSession(String sessionId) {
        AuctionSession session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        Map<String, Object> resp = finalizeSession(session, LocalDateTime.now());
        publishSessionEvent(session, "SESSION_ENDED", Map.of("reason", "MANUAL_STOP"));
        return resp;
    }

    public AuctionSession getLiveOrNextSessionInRoom(String roomId) {
        auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // 1) LIVE trước (status = 1)
        var liveOpt = auctionSessionRepository
                .findFirstByAuctionRoomIdAndStatusOrderByStartTimeDesc(roomId, 1);

        // 2) Không có LIVE -> lấy phiên kế tiếp (status = 0, chưa start)
        var s = liveOpt.orElseGet(() -> auctionSessionRepository
                .findFirstByAuctionRoomIdAndStatusAndStartTimeIsNullOrderByOrderIndexAsc(roomId, 0)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No session in this room")));

        // 3) Fallback ảnh nếu thiếu
        if ((s.getImageUrl() == null || s.getImageUrl().isBlank()) && s.getArtworkId() != null) {
            artworkRepository.findById(s.getArtworkId()).ifPresent(art -> {
                String img = null;
                if (art.getAvtArtwork() != null && !art.getAvtArtwork().isBlank()) {
                    img = art.getAvtArtwork();
                } else if (art.getImageUrls() != null && !art.getImageUrls().isEmpty()) {
                    img = art.getImageUrls().get(0);
                }
                if (img != null) {
                    s.setImageUrl(img); // set vào object trả về (không nhất thiết phải save)
                }
            });
        }

        return s;
    }

    /**
     * Lấy session hiện tại hoặc tiếp theo trong phòng, kèm thông tin username của người có giá cao nhất
     * 
     * @param roomId ID của phòng đấu giá
     * @return Map chứa tất cả thông tin session và highestBidderUsername
     */
    public Map<String, Object> getLiveOrNextSessionWithHighestBidder(String roomId) {
        // Lấy session
        AuctionSession session = getLiveOrNextSessionInRoom(roomId);
        
        // Tạo Map từ session
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", session.getId());
        result.put("auctionRoomId", session.getAuctionRoomId());
        result.put("artworkId", session.getArtworkId());
        result.put("imageUrl", session.getImageUrl());
        result.put("startTime", session.getStartTime());
        result.put("endedAt", session.getEndedAt());
        result.put("startingPrice", session.getStartingPrice());
        result.put("currentPrice", session.getCurrentPrice());
        result.put("status", session.getStatus());
        result.put("winnerId", session.getWinnerId());
        result.put("type", session.getType());
        result.put("viewCount", session.getViewCount());
        result.put("createdAt", session.getCreatedAt());
        result.put("updatedAt", session.getUpdatedAt());
        result.put("bidStep", session.getBidStep());
        result.put("durationSeconds", session.getDurationSeconds());
        result.put("durationMinutes", session.getDurationMinutes());
        result.put("maxDurationSeconds", session.getMaxDurationSeconds());
        result.put("extendStepSeconds", session.getExtendStepSeconds());
        result.put("extendThresholdSeconds", session.getExtendThresholdSeconds());
        result.put("finalPrice", session.getFinalPrice());
        result.put("bidCount", session.getBidCount());
        result.put("sellerId", session.getSellerId());
        result.put("orderIndex", session.getOrderIndex());
        
        // Lấy highest bid và username của người đặt giá cao nhất
        var highestBidOpt = bidsRepository.findTopByAuctionSessionIdOrderByAmountAtThatTimeDesc(session.getId());
        
        if (highestBidOpt.isPresent()) {
            Bids highestBid = highestBidOpt.get();
            result.put("highestBidderId", highestBid.getUserId());
            result.put("highestBidAmount", highestBid.getAmountAtThatTime());
            
            // Lấy username của highest bidder
            userRepository.findById(highestBid.getUserId()).ifPresent(user -> {
                result.put("highestBidderUsername", user.getUsername());
            });
        } else {
            result.put("highestBidderId", null);
            result.put("highestBidAmount", null);
            result.put("highestBidderUsername", null);
        }
        
        return result;
    }

    private AuctionSession startNextSessionInternal(String roomId) {
        auctionSessionRepository.findFirstByAuctionRoomIdAndStatus(roomId, SessionStatus.STARTED)
                .ifPresent(running -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A session is already running: " + running.getId());
                });

        var nextOpt = auctionSessionRepository
                .findFirstByAuctionRoomIdAndStatusAndStartTimeIsNullOrderByOrderIndexAsc(
                        roomId, SessionStatus.STOPPED);

        var next = nextOpt
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No session available to start"));

        LocalDateTime now = LocalDateTime.now();
        next.setStatus(SessionStatus.STARTED);
        next.setStartTime(now);
        next.setEndedAt(now.plusSeconds(resolveDurationSeconds(next)));
        next.setUpdatedAt(now);
        return auctionSessionRepository.save(next);
    }

    private Optional<AuctionSession> autoStartNextSession(String roomId) {
        try {
            AuctionSession started = startNextSessionInternal(roomId);
            publishSessionEvent(started, "SESSION_STARTED", Map.of("autoStart", true));
            return Optional.of(started);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND) || ex.getStatusCode().equals(HttpStatus.CONFLICT)) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    // 🧩 Hàm tạo note ngắn 12 ký tự
    @SuppressWarnings("unused")
    private String generateShortNote(String walletId) {
        String suffix = walletId.length() > 5 ? walletId.substring(walletId.length() - 5) : walletId;
        String millis = String.valueOf(System.currentTimeMillis());
        String last4 = millis.substring(millis.length() - 4);
        return "TP" + suffix + "_" + last4;
    }

    private Map<String, Object> finalizeSession(AuctionSession session, LocalDateTime endedAtOverride) {
        LocalDateTime endedAt = endedAtOverride != null ? endedAtOverride : LocalDateTime.now();
        session.setEndedAt(endedAt);
        session.setStatus(SessionStatus.STOPPED);
        session.setUpdatedAt(LocalDateTime.now());

        Bids highestBid = bidsRepository
                .findTopByAuctionSessionIdOrderByAmountAtThatTimeDesc(session.getId())
                .orElse(null);

        BigDecimal highestAmount = null;
        String winnerId = null;
        String winnerName = null;

        if (highestBid != null) {
            highestAmount = highestBid.getAmountAtThatTime();
            winnerId = highestBid.getUserId();
            User winner = userRepository.findById(winnerId).orElse(null);
            winnerName = (winner != null) ? winner.getUsername() : null;

            session.setWinnerId(winnerId);
            session.setFinalPrice(highestAmount);
        }

        Artwork artwork = (session.getArtworkId() != null)
                ? artworkRepository.findById(session.getArtworkId()).orElse(null)
                : null;

        auctionSessionRepository.save(session);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("sessionId", session.getId());
        resp.put("artworkId", artwork != null ? artwork.getId() : null);
        resp.put("artworkTitle", artwork != null ? artwork.getTitle() : null);
        resp.put("artworkImageUrl", artwork != null ? artwork.getAvtArtwork() : null);
        resp.put("winnerId", winnerId);
        resp.put("winnerName", winnerName);
        resp.put("highestBidAmount", highestAmount);
        resp.put("endedAt", session.getEndedAt());
        return resp;
    }

    private String url(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private int resolveMaxDurationSeconds(AuctionSessionCreateRequest request, int durationSeconds) {
        if (request != null && request.getStartTime() != null && request.getEndTime() != null) {
            long diff = Duration.between(request.getStartTime(), request.getEndTime()).getSeconds();
            if (diff > durationSeconds) {
                return (int) diff;
            }
        }
        return durationSeconds + DEFAULT_MAX_EXTENSION_BUFFER_SECONDS;
    }

    private int resolveDurationSeconds(AuctionSession session) {
        if (session != null && session.getDurationSeconds() != null && session.getDurationSeconds() > 0) {
            return session.getDurationSeconds();
        }
        return DEFAULT_SESSION_DURATION_MINUTES * 60;
    }

    private void publishSessionEvent(AuctionSession session, String eventType, Map<String, Object> extras) {
        if (messagingTemplate == null || session == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("sessionId", session.getId());
        payload.put("roomId", session.getAuctionRoomId());
        payload.put("orderIndex", session.getOrderIndex());
        payload.put("startTime", session.getStartTime());
        payload.put("endTime", session.getEndedAt());
        payload.put("currentPrice", session.safeCurrentPrice());
        payload.put("status", session.getStatus());
        if (extras != null) {
            payload.putAll(extras);
        }
        messagingTemplate.convertAndSend(SESSION_TOPIC_PREFIX + session.getAuctionRoomId(), payload);
    }

    @Scheduled(fixedDelayString = "${auction.session.monitor-interval-ms:1000}")
    @Transactional
    public void autoCloseSessionsWhenExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionSession> expired = auctionSessionRepository
                .findByStatusAndEndedAtLessThanEqual(SessionStatus.STARTED, now);
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(session -> {
            finalizeSession(session, now);
            publishSessionEvent(session, "SESSION_ENDED", Map.of("reason", "TIMEOUT"));
            autoStartNextSession(session.getAuctionRoomId());
        });
    }

    @SuppressWarnings("unused")
    private String generateTransactionId() {
        String random = Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 4);
        return "TXN-" + System.currentTimeMillis() + "-" + random;
    }

}
