package com.auctionaa.backend.Service;

import com.auctionaa.backend.Config.MbProps;
import com.auctionaa.backend.DTO.Request.AuctionSessionCreateRequest;
import com.auctionaa.backend.DTO.Request.StreamStartRequest;
import com.auctionaa.backend.Entity.*;
import com.auctionaa.backend.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.auctionaa.backend.Entity.SessionStatus.STARTED;
import static com.auctionaa.backend.Entity.SessionStatus.STOPPED;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public AuctionRoom startStream(String roomId){
        AuctionRoom room = auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
         room.setStatus(1);
         return auctionRoomRepository.save(room);
    }

    public AuctionRoom createdStream(StreamStartRequest rq, MultipartFile file) throws IOException {
        // Tạo phòng
        AuctionRoom room = new AuctionRoom();
        room.generateId();
        room.setAdminId(rq.getAdminId());
        room.setRoomName(rq.getRoomName());
        room.setStartedAt(LocalDateTime.now());
        room.setDescription(rq.getDescription());
        room.setType(rq.getType());
        room.setMemberIds(new ArrayList<>(List.of(rq.getAdminId())));
        room.setStatus(2); // CREATED
        room.setViewCount(0);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            CloudinaryService.UploadResult result =
                    cloudinaryService.uploadImage(file, "auctionaa/liveStream/" + room.getId(), "cover", null);
            room.setImageAuctionRoom(result.getUrl());
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
                AuctionSession session = new AuctionSession();
                session.generateId();
                session.setAuctionRoomId(savedRoom.getId());
                session.setArtworkId(s.getArtworkId());
                session.setImageUrl(s.getImageUrl());
                session.setStartingPrice(s.getStartingPrice());
                session.setCurrentPrice(s.getStartingPrice());
                session.setBidStep(s.getBidStep());
                session.setStatus(0); // DRAFT
                session.setOrderIndex(i++);
                session.setStartTime(null);     // mặc định null
                session.setEndedAt(null);
                session.setCreatedAt(LocalDateTime.now());
                session.setUpdatedAt(LocalDateTime.now());
                session.setBidCount(0);
                session.setViewCount(0);

                // chỉ lưu thời lượng (phút)
                session.setType(s.getDurationMinutes() + "m"); // lưu dạng text đơn giản, ví dụ "15m"

                sessions.add(session);
            }
            auctionSessionRepository.saveAll(sessions);
        }

        return savedRoom;
    }



    public Optional<AuctionRoom> getRoom(String roomId){
        Optional<AuctionRoom> roomOpt = auctionRoomRepository.findById(roomId);
        roomOpt.ifPresent(room -> {
            room.setViewCount((room.getViewCount()==null ? 0 : room.getViewCount())+1);
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
            if (topBidOpt.isEmpty()) continue;

            Bids topBid = topBidOpt.get();
            User winner = userRepository.findById(topBid.getUserId()).orElse(null);
            if (winner == null) continue;

            User artist = (artwork != null && artwork.getOwnerId() != null)
                    ? userRepository.findById(artwork.getOwnerId()).orElse(null) : null;

            BigDecimal artworkPrice = topBid.getAmountAtThatTime();
            BigDecimal buyerPremium = artworkPrice.multiply(BigDecimal.valueOf(0.15));
            BigDecimal insurance = BigDecimal.valueOf(125);
            BigDecimal shipping  = BigDecimal.valueOf(75);
            BigDecimal salesTax  = artworkPrice.multiply(BigDecimal.valueOf(0.085));
            BigDecimal total = artworkPrice.add(buyerPremium).add(insurance).add(shipping).add(salesTax);

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
                    url(total.toPlainString()),   // tổng tiền phải thanh toán
                    url(paymentNote),             // nội dung chuyển khoản để đối soát
                    (mbProps.getAccountName() != null && !mbProps.getAccountName().isBlank())
                            ? "&accountName=" + url(mbProps.getAccountName()) : ""
            );

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
                    total.toPlainString()
            ));
            // Nếu cần deep link:
            // noti.setLink("/invoices/" + invoice.getId());
            notifications.add(noti);
        }

        invoiceRepository.saveAll(invoices);
        if (!notifications.isEmpty()) notificationService.addAll(notifications);

        return invoices;
    }



    public AuctionSession startNextSession(String roomId) {
        // 1) Chặn nếu đã có phiên đang chạy
        auctionSessionRepository.findFirstByAuctionRoomIdAndStatus(roomId, SessionStatus.STARTED)
                .ifPresent(running -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A session is already running: " + running.getId());
                });

        // 2) Tìm phiên SẴN SÀNG đầu tiên: status = 0 **và chưa từng start** (startTime == null)
        var nextOpt = auctionSessionRepository
                .findFirstByAuctionRoomIdAndStatusAndStartTimeIsNullOrderByOrderIndexAsc(
                        roomId, SessionStatus.STOPPED);

        var next = nextOpt.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No session available to start"));

        // 3) Cho chạy + set startTime (không bao giờ reset về null nữa)
        next.setStatus(SessionStatus.STARTED);
        next.setStartTime(LocalDateTime.now());
        return auctionSessionRepository.save(next);
    }




    public Map<String, Object> stopAuctionSession(String sessionId) {
        AuctionSession session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // cập nhật kết thúc
        session.setEndedAt(LocalDateTime.now());
        session.setStatus(0);

        // bid cao nhất (có thể null)
        Bids highestBid = bidsRepository
                .findTopByAuctionSessionIdOrderByAmountAtThatTimeDesc(sessionId)
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

        // artwork (có thể null)
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
        resp.put("endedAt", session.getEndedAt());   // đã set ở trên
        return resp;
    }


    public AuctionSession getLiveOrNextSessionInRoom(String roomId) {
        auctionRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // 1) LIVE trước (status = 1)
        var liveOpt = auctionSessionRepository
                .findFirstByAuctionRoomIdAndStatusOrderByStartTimeDesc(roomId, 1);

        // 2) Không có LIVE -> lấy phiên kế tiếp (status = 0, chưa start)
        var s = liveOpt.orElseGet(() ->
                auctionSessionRepository
                        .findFirstByAuctionRoomIdAndStatusAndStartTimeIsNullOrderByOrderIndexAsc(roomId, 0)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No session in this room"))
        );

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
                    s.setImageUrl(img);  // set vào object trả về (không nhất thiết phải save)
                }
            });
        }

        return s;
    }

    // 🧩 Hàm tạo note ngắn 12 ký tự
    private String generateShortNote(String walletId) {
        String suffix = walletId.length() > 5 ? walletId.substring(walletId.length() - 5) : walletId;
        String millis = String.valueOf(System.currentTimeMillis());
        String last4 = millis.substring(millis.length() - 4);
        return "TP" + suffix + "_" + last4;
    }

    private String url(String s) {
        try { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private String generateTransactionId() {
        String random = Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 4);
        return "TXN-" + System.currentTimeMillis() + "-" + random;
    }

}
