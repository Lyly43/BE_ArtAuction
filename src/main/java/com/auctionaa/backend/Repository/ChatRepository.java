package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ChatRepository extends MongoRepository<Chat, String> {
    List<Chat> findByRoomIdOrderBySentAtDesc(String roomId, Pageable pageable);

    List<Chat> findByAuctionIdAndTypeOrderBySentAtDesc(String auctionId, Chat.ChatType type, Pageable pageable);

    // Query để lấy tin nhắn SUPPORT cho user cụ thể trong room
    @Query(value = "{ 'type': 'SUPPORT', 'roomId': ?0, $or: [ " +
            " { 'senderId': ?1 }, " +                 // user gửi
            " { 'receiverId': ?1 }, " +               // admin gửi riêng cho user
            " { 'senderId': ?2, 'receiverId': null }" // admin broadcast
            + " ] }",
            sort = "{ 'sentAt': -1 }")
    List<Chat> findSupportMessagesForUser(String roomId, String userId, String adminId, Pageable pageable);
}
