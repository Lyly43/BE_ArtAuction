package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Response.ChatWithUserResponse;
import com.auctionaa.backend.Entity.Chat;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    // ========== REST: load lịch sử chat ==========
    @GetMapping("/global/{roomId}/messages")
    public List<ChatWithUserResponse> globalChatHistory(@PathVariable String roomId,
                                                        @RequestParam(defaultValue = "50") int limit) {
        return chatService.recent("global:" + roomId, limit);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatWithUserResponse> roomHistory(@PathVariable String roomId,
                                                  @RequestParam(defaultValue = "50") int limit) {
        return chatService.recent(roomId, limit);
    }

    // ========== STOMP: Global chat (tất cả user chat với nhau trong phòng đấu giá) ==========
    @MessageMapping("/global.send.{roomId}")
    public void sendGlobal(@DestinationVariable String roomId,
                           @Payload ChatSendDTO dto,
                           Principal principal,
                           SimpMessageHeaderAccessor headerAccessor) {
        // ✅ Khôi phục Principal nếu bị mất do SockJS
        if (principal == null && headerAccessor.getSessionAttributes() != null) {
            principal = (Principal) headerAccessor.getSessionAttributes().get("SPRING.PRINCIPAL");
        }

        if (principal == null) {
            System.out.println("[GLOBAL SEND] rejected: missing Principal");
            throw new IllegalStateException("Unauthenticated STOMP SEND (missing Principal)");
        }

        String sender = principal.getName();
        System.out.println("[GLOBAL SEND] from user=" + sender + " to room=" + roomId);

        Chat c = new Chat();
        c.setType(Chat.ChatType.GLOBAL);
        c.setRoomId("global:" + roomId); // Chat công khai của phòng đấu giá cụ thể
        c.setAuctionId(roomId); // roomId chính là auctionId
        c.setSenderId(sender);
        c.setContent(dto.getContent());

        chatService.saveAndBroadcast(c);
    }

    // ========== STOMP: Room channel (admin ↔ users trong phòng đấu giá) ==========
    @MessageMapping("/room.send.{roomId}")
    public void sendRoom(@DestinationVariable String roomId,
                         @Payload ChatSendDTO dto,
                         Principal principal,
                         SimpMessageHeaderAccessor headerAccessor) {

        // ✅ Khôi phục Principal nếu bị mất
        if (principal == null && headerAccessor.getSessionAttributes() != null) {
            principal = (Principal) headerAccessor.getSessionAttributes().get("SPRING.PRINCIPAL");
        }

        if (principal == null) {
            System.out.println("[ROOM SEND] rejected: missing Principal");
            throw new IllegalStateException("Unauthenticated STOMP SEND (missing Principal)");
        }

        String sender = principal.getName();
        System.out.println("[ROOM SEND] from=" + sender + " to room=" + roomId);

        Chat c = new Chat();
        c.setType(Chat.ChatType.SUPPORT); // Chat hỗ trợ trong phòng đấu giá
        c.setRoomId(roomId); // Room ID của phòng đấu giá
        c.setSenderId(sender);
        c.setReceiverId(dto.getReceiverId()); // null nếu admin gửi cho tất cả, adminId nếu user gửi cho admin
        c.setAuctionId(dto.getAuctionId());
        c.setContent(dto.getContent());

        chatService.saveAndBroadcast(c);
    }

    // ========== DTO ==========
    @Data
    public static class ChatSendDTO {
        private String content;
        private Chat.ChatType type; // GLOBAL/DIRECT/SUPPORT
        private String receiverId;
        private String auctionId;
    }
}
