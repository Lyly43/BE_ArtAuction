package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Response.PlaceBidResponse;
import com.auctionaa.backend.Entity.*;
import com.auctionaa.backend.Repository.UserRepository;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BidService {
    private final MongoTemplate mongo;
    private final SimpMessagingTemplate ws;
    private final UserRepository userRepo;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int DEFAULT_EXTEND_THRESHOLD_SECONDS = 60;
    private static final int DEFAULT_EXTEND_STEP_SECONDS = 120;

    // @Transactional
    // public PlaceBidResponse placeBid(String userId, String sessionId,
    // BigDecimal amount, String idempotencyKey) {
    //
    // LocalDateTime now = LocalDateTime.now();
    // PlaceBidResponse res = new PlaceBidResponse();
    // res.setBidTime(now);
    //
    // // 0) Idempotency
    // try {
    // Idempotency idem = new Idempotency();
    // idem.setUserId(userId);
    // idem.setKey(idempotencyKey);
    // idem.setSessionId(sessionId);
    // idem.setAmount(nvl(amount));
    // mongo.insert(idem);
    // } catch (DuplicateKeyException ex) {
    // AuctionSession s = mongo.findById(sessionId, AuctionSession.class);
    // String curLeaderUsername = resolveUsername(s != null ? s.getWinnerId() :
    // null);
    // return rejectWithLeader(res,
    // "Duplicate idempotencyKey",
    // s != null ? nvl(safe(s)) : ZERO,
    // curLeaderUsername,
    // 0);
    // }
    //
    // // 1) Đọc phiên & kiểm tra
    // AuctionSession session = mongo.findById(sessionId, AuctionSession.class);
    // if (session == null) return rejectWithLeader(res, "Session not found", ZERO,
    // null, 0);
    // if (session.getStatus() != 1)
    // return rejectWithLeader(res, "Session is not LIVE", safe(session),
    // resolveUsername(session.getWinnerId()), 0);
    //
    // BigDecimal currentPrice = safe(session);
    // BigDecimal bidStep = nvl(session.getBidStep());
    // amount = nvl(amount);
    //
    // BigDecimal minAccept = currentPrice.add(bidStep);
    // if (amount.compareTo(minAccept) < 0)
    // return rejectWithLeader(res,
    // "Bid too low. Min = " + minAccept.toPlainString(),
    // currentPrice,
    // resolveUsername(session.getWinnerId()),
    // 0);
    //
    // String bidderUsername = resolveUsername(userId);
    //
    // // chặn leader tự bid tiếp (DB winnerId lưu userId)
    // if (Objects.equals(session.getWinnerId(), userId))
    // return rejectWithLeader(res,
    // "You are already the current leader. Wait for another bid.",
    // currentPrice,
    // bidderUsername,
    // 0);
    //
    // // 2) Kiểm tra ví
    // Wallet wallet = mongo.findOne(new Query(Criteria.where("userId").is(userId)),
    // Wallet.class);
    // if (wallet == null)
    // return rejectWithLeader(res, "Wallet not found", currentPrice,
    // resolveUsername(session.getWinnerId()), 0);
    //
    // BigDecimal balance = nvl(wallet.getBalance());
    // BigDecimal frozen = nvl(wallet.getFrozenBalance());
    // BigDecimal available = balance.subtract(frozen);
    // if (available.compareTo(amount) < 0)
    // return rejectWithLeader(res, "Insufficient balance", currentPrice,
    // resolveUsername(session.getWinnerId()), 0);
    //
    // // 3) CAS: currentPrice < amount && winnerId != userId
    // Query q = new Query(Criteria.where("_id").is(sessionId)
    // .and("status").is(1)
    // .and("currentPrice").lt(amount)
    // .and("winnerId").ne(userId));
    // Update u = new Update()
    // .set("currentPrice", amount)
    // .set("winnerId", userId) // LƯU userId vào DB
    // .set("updatedAt", now);
    //
    // AuctionSession oldSession = mongo.findAndModify(q, u,
    // FindAndModifyOptions.options().returnNew(false),
    // AuctionSession.class);
    //
    // if (oldSession == null) {
    // // Outbid
    // AuctionSession latest = mongo.findById(sessionId, AuctionSession.class);
    // res.setResult(-1);
    // res.setCurrentPrice(latest != null ? nvl(latest.safeCurrentPrice()) :
    // currentPrice);
    // res.setLeader(resolveUsername(latest != null ? latest.getWinnerId() : null));
    // res.setMessage("Someone bid faster");
    // saveIdemStatus(userId, idempotencyKey, res);
    // return res;
    // }
    //
    // // leader cũ theo userId (DB)
    // String prevLeaderUserId = oldSession.getWinnerId();
    // BigDecimal oldPrice = nvl(oldSession.getCurrentPrice());
    //
    // // 4) Frozen balance
    // if (prevLeaderUserId != null && oldPrice.compareTo(ZERO) > 0) {
    // mongo.updateFirst(
    // new Query(Criteria.where("userId").is(prevLeaderUserId)),
    // new Update().inc("frozen_balance", d128(oldPrice.negate()))
    // .set("updatedAt", now),
    // Wallet.class);
    // }
    // if (amount.compareTo(ZERO) > 0) {
    // mongo.updateFirst(
    // new Query(Criteria.where("userId").is(userId)),
    // new Update().inc("frozen_balance", d128(amount))
    // .set("updatedAt", now),
    // Wallet.class);
    // }
    //
    // // 5) Log bid
    // Bids bid = new Bids();
    // bid.setAuctionSessionId(sessionId);
    // bid.setUserId(userId);
    // bid.setAmountAtThatTime(amount);
    // bid.setBidTime(now);
    // mongo.insert(bid);
    //
    // // 6) Success
    // res.setResult(1);
    // res.setCurrentPrice(amount);
    // res.setLeader(bidderUsername);
    // res.setMessage("Accepted");
    //
    // Map<String, Object> payload = Map.of(
    // "sessionId", sessionId,
    // "price", amount,
    // "leader", bidderUsername,
    // "at", now.toString()
    // );
    // ws.convertAndSend("/topic/auction." + sessionId + ".bids", payload);
    //
    // saveIdemStatus(userId, idempotencyKey, res);
    // return res;
    // }
    //
    private PlaceBidResponse rejectWithLeader(PlaceBidResponse res, String msg, BigDecimal currentPrice,
                                              String leaderUsername, int code, LocalDateTime sessionEndTime) {
        res.setResult(code);
        res.setMessage(msg);
        res.setCurrentPrice(nvl(currentPrice));
        res.setLeader(leaderUsername);
        if (res.getBidTime() == null)
            res.setBidTime(LocalDateTime.now());
        res.setSessionEndTime(sessionEndTime);
        res.setExtended(false);
        return res;
    }

    private ExtensionResult extendSessionIfNeeded(String sessionId, AuctionSession snapshot, LocalDateTime now) {
        if (snapshot == null) {
            return new ExtensionResult(false, null);
        }
        LocalDateTime currentEnd = snapshot.getEndedAt();
        if (currentEnd == null) {
            return new ExtensionResult(false, null);
        }
        long secondsLeft = Duration.between(now, currentEnd).getSeconds();
        if (secondsLeft >= 0 && secondsLeft <= resolveExtendThreshold(snapshot)) {
            LocalDateTime candidate = currentEnd.plusSeconds(resolveExtendStep(snapshot));
            LocalDateTime cap = snapshot.computeMaxEndTime();
            LocalDateTime targetEnd = candidate;
            if (cap != null && candidate.isAfter(cap)) {
                targetEnd = cap;
            }
            if (!targetEnd.isAfter(currentEnd)) {
                return new ExtensionResult(false, currentEnd);
            }
            Query extendQuery = new Query(Criteria.where("_id").is(sessionId)
                    .and("endedAt").is(currentEnd));
            Update extendUpdate = new Update()
                    .set("endedAt", targetEnd)
                    .set("updatedAt", now);
            UpdateResult result = mongo.updateFirst(extendQuery, extendUpdate, AuctionSession.class);
            if (result.getModifiedCount() > 0) {
                return new ExtensionResult(true, targetEnd);
            }
        }
        return new ExtensionResult(false, currentEnd);
    }

    private int resolveExtendThreshold(AuctionSession session) {
        Integer threshold = session.getExtendThresholdSeconds();
        return (threshold != null && threshold > 0) ? threshold : DEFAULT_EXTEND_THRESHOLD_SECONDS;
    }

    private int resolveExtendStep(AuctionSession session) {
        Integer step = session.getExtendStepSeconds();
        return (step != null && step > 0) ? step : DEFAULT_EXTEND_STEP_SECONDS;
    }

    private record ExtensionResult(boolean extended, LocalDateTime endTime) {
    }

    private void saveIdemStatus(String userId, String idempotencyKey, PlaceBidResponse res) {
        Query q = new Query(Criteria.where("userId").is(userId)
                .and("key").is(idempotencyKey));
        Update u = new Update().set("status", res.getResult());
        mongo.updateFirst(q, u, Idempotency.class);
    }

    private String resolveUsername(String userIdOrNull) {
        if (userIdOrNull == null || userIdOrNull.isBlank())
            return null;
        return userRepo.findById(userIdOrNull)
                .map(User::getUsername)
                .orElse(userIdOrNull);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? ZERO : v;
    }

    private static BigDecimal safe(AuctionSession s) {
        BigDecimal cur = s.getCurrentPrice();
        if (cur != null)
            return cur;
        return s.getStartingPrice() != null ? s.getStartingPrice() : ZERO;
    }

    @Transactional
    public PlaceBidResponse placeBidByRoom(String userId, String auctionRoomId,
                                           BigDecimal amount, String idempotencyKey) {
        LocalDateTime now = LocalDateTime.now();
        PlaceBidResponse res = new PlaceBidResponse();
        res.setBidTime(now);

        // 1) Tìm phiên đang ACTIVE trong phòng tại thời điểm now
        // Điều kiện: status = 1 và startTime <= now
        Query findActive = new Query(Criteria.where("auctionRoomId").is(auctionRoomId)
                .and("status").is(1)
                .and("startTime").lte(now));
        // Nếu có nhiều phiên khởi động trước đó mà vẫn để status=1 (không nên), lấy
        // phiên startTime mới nhất
        findActive.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "startTime"));

        AuctionSession session = mongo.findOne(findActive, AuctionSession.class);

        if (session == null) {
            // Không có phiên nào thỏa: hoặc chưa tới giờ bắt đầu, hoặc không có phiên
            // status=1
            return rejectWithLeader(res,
                    "No active session in this room at this time",
                    ZERO, null, 0, null);
        }

        String sessionId = session.getId(); // sử dụng _id của phiên vừa tìm được

        // 2) Idempotency (thực hiện SAU khi xác định sessionId)
        try {
            Idempotency idem = new Idempotency();
            idem.setUserId(userId);
            idem.setKey(idempotencyKey);
            idem.setSessionId(sessionId); // gắn sessionId thực
            idem.setAmount(nvl(amount));
            mongo.insert(idem);
        } catch (DuplicateKeyException ex) {
            // key đã dùng trước đó: trả về trạng thái hiện tại của phiên
            AuctionSession s = mongo.findById(sessionId, AuctionSession.class);
            String curLeaderUsername = resolveUsername(s != null ? s.getWinnerId() : null);
            return rejectWithLeader(res,
                    "Duplicate idempotencyKey",
                    s != null ? nvl(safe(s)) : ZERO,
                    curLeaderUsername,
                    0,
                    s != null ? s.getEndedAt() : null);
        }

        // 3) Kiểm tra tình trạng phiên (phòng bảo đảm chỉ 1 phiên LIVE)
        if (session.getStatus() != 1) {
            return rejectWithLeader(res,
                    "Session is not LIVE",
                    safe(session),
                    resolveUsername(session.getWinnerId()),
                    0,
                    session.getEndedAt());
        }

        // 4) Kiểm tra giá hợp lệ
        BigDecimal currentPrice = safe(session);
        BigDecimal bidStep = nvl(session.getBidStep());
        amount = nvl(amount);

        BigDecimal minAccept = currentPrice.add(bidStep);
        if (amount.compareTo(minAccept) < 0) {
            return rejectWithLeader(res,
                    "Bid too low. Min = " + minAccept.toPlainString(),
                    currentPrice,
                    resolveUsername(session.getWinnerId()),
                    0,
                    session.getEndedAt());
        }

        String bidderUsername = resolveUsername(userId);

        // Chặn leader tự bid tiếp
        if (Objects.equals(session.getWinnerId(), userId)) {
            return rejectWithLeader(res,
                    "You are already the current leader. Wait for another bid.",
                    currentPrice,
                    bidderUsername,
                    0,
                    session.getEndedAt());
        }

        // 5) CAS: currentPrice < amount && winnerId != userId && phiên vẫn LIVE
        Query q = new Query(Criteria.where("_id").is(sessionId)
                .and("status").is(1)
                .and("currentPrice").lt(amount)
                .and("winnerId").ne(userId));

        Update u = new Update()
                .set("currentPrice", amount)
                .set("winnerId", userId) // lưu userId vào DB
                .set("updatedAt", now);

        AuctionSession oldSession = mongo.findAndModify(q, u,
                FindAndModifyOptions.options().returnNew(false),
                AuctionSession.class);

        if (oldSession == null) {
            // Outbid hoặc phiên chuyển trạng thái trong lúc race
            AuctionSession latest = mongo.findById(sessionId, AuctionSession.class);
            res.setResult(-1);
            res.setCurrentPrice(latest != null ? nvl(latest.safeCurrentPrice()) : currentPrice);
            res.setLeader(resolveUsername(latest != null ? latest.getWinnerId() : null));
            res.setMessage("Someone bid faster");
            res.setSessionEndTime(latest != null ? latest.getEndedAt() : null);
            res.setExtended(false);
            saveIdemStatus(userId, idempotencyKey, res);
            return res;
        }

        ExtensionResult extension = extendSessionIfNeeded(sessionId, oldSession, now);
        LocalDateTime effectiveEndTime = extension.endTime();
        if (effectiveEndTime != null) {
            session.setEndedAt(effectiveEndTime);
        }
        long remainingSeconds = effectiveEndTime != null
                ? Math.max(0, Duration.between(now, effectiveEndTime).getSeconds())
                : -1;

        // 6) Log bid
        Bids bid = new Bids();
        bid.setAuctionSessionId(sessionId);
        bid.setUserId(userId);
        bid.setAmountAtThatTime(amount);
        bid.setBidTime(now);
        mongo.insert(bid);

        // 7) Success + đẩy WS
        res.setResult(1);
        res.setCurrentPrice(amount);
        res.setLeader(bidderUsername);
        res.setMessage("Accepted");
        res.setSessionEndTime(effectiveEndTime);
        res.setExtended(extension.extended());

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "BID_ACCEPTED");
        payload.put("sessionId", sessionId);
        payload.put("roomId", session.getAuctionRoomId());
        payload.put("price", amount);
        payload.put("leader", bidderUsername);
        payload.put("at", now.toString());
        payload.put("extended", extension.extended());
        if (effectiveEndTime != null) {
            payload.put("endTime", effectiveEndTime);
            payload.put("remainingSeconds", remainingSeconds >= 0 ? remainingSeconds : null);
        }
        ws.convertAndSend("/topic/auction." + sessionId + ".bids", payload);

        saveIdemStatus(userId, idempotencyKey, res);
        return res;
    }

}
