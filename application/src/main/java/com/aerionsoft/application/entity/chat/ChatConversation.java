package com.aerionsoft.application.entity.chat;

import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import com.aerionsoft.application.util.UserDateTimeUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "assigned_admin_id")
    private Long assignedAdminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChatConversationStatus status;

    @Column(length = 255)
    private String subject;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "closed_by_type", length = 16)
    private ChatSenderType closedByType;

    @Column(name = "closed_by_id")
    private Long closedById;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
        createdTimeOffset = UserDateTimeUtil.currentOffset();
        updatedTimeOffset = UserDateTimeUtil.currentOffset();
        if (status == null) {
            status = ChatConversationStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
        updatedTimeOffset = UserDateTimeUtil.currentOffset();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setConversation(this);
        lastMessageAt = UserDateTimeUtil.now();
    }
}
