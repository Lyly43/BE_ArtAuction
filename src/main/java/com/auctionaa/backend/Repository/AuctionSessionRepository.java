package com.auctionaa.backend.Repository;
import com.auctionaa.backend.Entity.AuctionSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionSessionRepository extends MongoRepository<AuctionSession, String> {
    Optional<AuctionSession> findFirstByAuctionRoomIdAndStatus(String roomId, int status);

    Optional<AuctionSession>
    findFirstByAuctionRoomIdAndStatusOrderByOrderIndexAsc(String roomId, int status);

    // ✅ Trả về danh sách tất cả session thuộc 1 phòng
    List<AuctionSession> findByAuctionRoomId(String auctionRoomId);
    // AuctionSessionRepository
    Optional<AuctionSession>
    findFirstByAuctionRoomIdAndStatusAndStartTimeIsNullOrderByOrderIndexAsc(String roomId, int status);

    // ✅ Tìm tất cả session đang LIVE trong 1 phòng
    List<AuctionSession> findByAuctionRoomIdAndStatus(String auctionRoomId, int status);

    // phiên đang LIVE trong phòng (ưu tiên phiên startTime mới nhất)
    Optional<AuctionSession>
    findFirstByAuctionRoomIdAndStatusOrderByStartTimeDesc(String auctionRoomId, int status);

    // (tuỳ chọn) kiểm tra còn bao nhiêu phiên chưa chạy/đang chạy
    long countByAuctionRoomIdAndStatusIn(String auctionRoomId, List<Integer> statuses);



}
