package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Response.ChatWithUserResponse;
import com.auctionaa.backend.Entity.Chat;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.ChatRepository;
import com.auctionaa.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository repo;
    private final SimpMessagingTemplate broker;
    private final UserRepository userRepository;

    public Chat saveAndBroadcast(Chat chat) {
        chat.setSentAt(Instant.now());
        chat.setStatus(Chat.MessageStatus.SENT);
        Chat saved = repo.save(chat);

        // Enrich message với thông tin user trước khi broadcast
        ChatWithUserResponse enrichedMessage = enrichWithUserInfo(saved);

        // Phân loại broadcast theo loại chat
        if (saved.getType() == Chat.ChatType.GLOBAL) {
            // Chat công khai của phòng đấu giá - gửi đến topic global.{auctionId}
            broker.convertAndSend("/topic/global." + saved.getAuctionId(), enrichedMessage);
        } else {
            // Chat trong phòng đấu giá hoặc chat trực tiếp - gửi đến topic room
            broker.convertAndSend("/topic/room." + saved.getRoomId(), enrichedMessage);
        }
        return saved;
    }

    public List<ChatWithUserResponse> recent(String roomId, int limit) {
        List<Chat> chats = repo.findByRoomIdOrderBySentAtDesc(roomId, PageRequest.of(0, limit));
        return chats.stream()
                .map(this::enrichWithUserInfo)
                .collect(Collectors.toList());
    }

    // Lấy tin nhắn SUPPORT cho user cụ thể trong room
    public List<ChatWithUserResponse> getSupportMessagesForUser(String roomId, String userId, String adminId, int limit) {
        List<Chat> chats = repo.findSupportMessagesForUser(roomId, userId, adminId, PageRequest.of(0, limit));
        return chats.stream()
                .map(this::enrichWithUserInfo)
                .collect(Collectors.toList());
    }

    // Enrich chat message với thông tin user
    private ChatWithUserResponse enrichWithUserInfo(Chat chat) {
        String senderName = null;
        String senderEmail = null;

        if (chat.getSenderId() != null) {
            Optional<User> user = userRepository.findById(chat.getSenderId());
            if (user.isPresent()) {
                User u = user.get();
                // Lấy username và email từ User table
                senderName = u.getUsername();
                senderEmail = u.getEmail();
                
                System.out.println("🔍 Found user: " + u.getId() + " -> username: " + u.getUsername() + ", email: " + u.getEmail());
            } else {
                System.out.println("❌ User not found for senderId: " + chat.getSenderId());
                // Nếu không tìm thấy user, fallback về senderId
                senderEmail = chat.getSenderId();
            }
        }

        return new ChatWithUserResponse(chat, senderName, senderEmail);
    }
}
