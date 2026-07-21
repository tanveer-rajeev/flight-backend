package com.aerionsoft.application.entity.chat;

import com.aerionsoft.application.enums.chat.ChatSenderType;
import com.aerionsoft.application.util.UserDateTimeUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 16)
    private ChatSenderType senderType;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name", length = 255)
    private String senderName;

    @Column(columnDefinition = "TEXT")
    private String body;

    /** Comma-separated public file URLs (uploaded via /api/files first). */
    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments;

    /** Admin-only internal note; never exposed to the client user. */
    @Column(name = "is_internal", nullable = false)
    @Builder.Default
    private Boolean isInternal = false;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        createdTimeOffset = UserDateTimeUtil.currentOffset();
        if (isRead == null) {
            isRead = false;
        }
        if (isInternal == null) {
            isInternal = false;
        }
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = UserDateTimeUtil.now();
    }
}
