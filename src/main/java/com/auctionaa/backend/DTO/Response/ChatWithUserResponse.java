package com.auctionaa.backend.DTO.Response;

import com.auctionaa.backend.Entity.Chat;
import lombok.Data;

@Data
public class ChatWithUserResponse {
    private String id;
    private String content;
    private String sentAt;
    private String senderId;
    private String senderName;
    private String senderEmail;
    private String receiverId;
    private String auctionId;
    private String roomId;
    private Chat.ChatType type;
    private Chat.MessageStatus status;

    public ChatWithUserResponse(Chat chat, String senderName, String senderEmail) {
        this.id = chat.getId();
        this.content = chat.getContent();
        this.sentAt = chat.getSentAt() != null ? chat.getSentAt().toString() : null;
        this.senderId = chat.getSenderId();
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.receiverId = chat.getReceiverId();
        this.auctionId = chat.getAuctionId();
        this.roomId = chat.getRoomId();
        this.type = chat.getType();
        this.status = chat.getStatus();
    }
}

