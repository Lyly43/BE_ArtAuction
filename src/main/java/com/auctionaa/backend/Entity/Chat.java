package com.auctionaa.backend.Entity;

import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Document(collection = "chats")
@AllArgsConstructor @NoArgsConstructor
@CompoundIndexes({
        @CompoundIndex(def = "{'roomId': 1, 'sentAt': 1}"),
        @CompoundIndex(def = "{'senderId': 1, 'sentAt': -1}")
})
public class Chat extends BaseEntity {

    public enum ChatType { GLOBAL, DIRECT, SUPPORT }
    public enum MessageStatus { SENT, DELIVERED, READ }

    private String content;
    private Instant sentAt;             // server time để tránh lệch TZ

    private String senderId;            // dùng email từ JWT hoặc map ra userId
    private String receiverId;          // DIRECT: admin hoặc user khác; GLOBAL: null
    private String auctionId;           // SUPPORT: gắn phiên (có thể null nếu DIRECT)

    private String roomId;              // "global" | "dm:{user}-{admin}" | "support:{auction}-{user}"
    private ChatType type;              // GLOBAL/DIRECT/SUPPORT
    private MessageStatus status;       // SENT/DELIVERED/READ

    @Override public String getPrefix() { return "Chat-"; }
}
