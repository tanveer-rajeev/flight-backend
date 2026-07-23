package com.aerionsoft.application.service.chat;

import com.aerionsoft.application.dto.chat.ChatConversationStatsDTO;
import com.aerionsoft.application.entity.chat.ChatConversation;
import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import com.aerionsoft.application.repository.chat.ChatMessageRepository;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.util.UserDateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
final class ChatConversationStatsSupport {

    private final ChatMessageRepository messageRepository;
    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;

    record MessageCounts(long userPublic, long adminPublic, long adminInternal) {
        long totalPublic() {
            return userPublic + adminPublic;
        }
    }

    record StatsContext(
            Map<Long, MessageCounts> messageCountsByConversation,
            Map<Long, LocalDateTime> firstAdminReplyByConversation) {
    }

    StatsContext loadContext(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return new StatsContext(Map.of(), Map.of());
        }
        Map<Long, MessageCounts> counts = new HashMap<>();
        for (Object[] row : messageRepository.countGroupedByConversationIds(conversationIds)) {
            Long conversationId = (Long) row[0];
            ChatSenderType senderType = (ChatSenderType) row[1];
            boolean internal = Boolean.TRUE.equals(row[2]);
            long count = (Long) row[3];
            MessageCounts current = counts.getOrDefault(conversationId, new MessageCounts(0, 0, 0));
            if (senderType == ChatSenderType.USER) {
                counts.put(conversationId, new MessageCounts(current.userPublic() + count,
                        current.adminPublic(), current.adminInternal()));
            } else if (internal) {
                counts.put(conversationId, new MessageCounts(current.userPublic(),
                        current.adminPublic(), current.adminInternal() + count));
            } else {
                counts.put(conversationId, new MessageCounts(current.userPublic(),
                        current.adminPublic() + count, current.adminInternal()));
            }
        }

        Map<Long, LocalDateTime> firstAdminReply = new HashMap<>();
        for (Object[] row : messageRepository.findFirstAdminReplyAtByConversationIds(conversationIds)) {
            firstAdminReply.put((Long) row[0], (LocalDateTime) row[1]);
        }
        return new StatsContext(counts, firstAdminReply);
    }

    ChatConversationStatsDTO build(ChatConversation conversation, boolean adminView, StatsContext context) {
        LocalDateTime now = UserDateTimeUtil.now();
        LocalDateTime createdAt = conversation.getCreatedAt();
        LocalDateTime endAt = conversation.getClosedAt() != null ? conversation.getClosedAt() : now;
        LocalDateTime claimedAt = conversation.getClaimedAt();

        MessageCounts counts = context != null
                ? context.messageCountsByConversation().getOrDefault(conversation.getId(), new MessageCounts(0, 0, 0))
                : loadContext(List.of(conversation.getId()))
                        .messageCountsByConversation()
                        .getOrDefault(conversation.getId(), new MessageCounts(0, 0, 0));

        LocalDateTime firstAdminReplyAt = context != null
                ? context.firstAdminReplyByConversation().get(conversation.getId())
                : messageRepository.findFirstAdminReplyAt(conversation.getId()).orElse(null);

        Long durationSeconds = secondsBetween(createdAt, endAt);
        Long waitTimeSeconds = computeWaitTimeSeconds(conversation, claimedAt, firstAdminReplyAt, now);
        Long activeDurationSeconds = computeActiveDurationSeconds(conversation, claimedAt, endAt, now);

        ChatConversationStatsDTO.ChatConversationStatsDTOBuilder builder = ChatConversationStatsDTO.builder()
                .claimedAt(claimedAt)
                .firstAdminReplyAt(firstAdminReplyAt)
                .durationSeconds(durationSeconds)
                .waitTimeSeconds(waitTimeSeconds)
                .activeDurationSeconds(activeDurationSeconds)
                .totalMessageCount(counts.totalPublic())
                .userMessageCount(counts.userPublic())
                .adminMessageCount(counts.adminPublic())
                .closedByName(resolveClosedByName(conversation));

        if (adminView) {
            builder.internalNoteCount(counts.adminInternal());
        }

        return builder.build();
    }

    private Long computeWaitTimeSeconds(ChatConversation conversation,
                                        LocalDateTime claimedAt,
                                        LocalDateTime firstAdminReplyAt,
                                        LocalDateTime now) {
        if (createdAtMissing(conversation)) {
            return null;
        }
        LocalDateTime createdAt = conversation.getCreatedAt();
        if (conversation.getStatus() == ChatConversationStatus.OPEN) {
            return secondsBetween(createdAt, now);
        }
        LocalDateTime joinedAt = claimedAt != null ? claimedAt : firstAdminReplyAt;
        if (joinedAt == null) {
            return null;
        }
        return secondsBetween(createdAt, joinedAt);
    }

    private Long computeActiveDurationSeconds(ChatConversation conversation,
                                              LocalDateTime claimedAt,
                                              LocalDateTime endAt,
                                              LocalDateTime now) {
        if (claimedAt == null) {
            return null;
        }
        LocalDateTime activeEnd = conversation.getStatus() == ChatConversationStatus.OPEN
                ? claimedAt
                : (conversation.getStatus() == ChatConversationStatus.ACTIVE ? now : endAt);
        if (activeEnd.isBefore(claimedAt)) {
            return 0L;
        }
        return secondsBetween(claimedAt, activeEnd);
    }

    private String resolveClosedByName(ChatConversation conversation) {
        if (conversation.getClosedByType() == null || conversation.getClosedById() == null) {
            return null;
        }
        if (conversation.getClosedByType() == ChatSenderType.ADMIN) {
            return adminUserRepository.findById(conversation.getClosedById())
                    .map(a -> displayName(a.getFullName(), a.getEmail()))
                    .orElse(null);
        }
        return userRepository.findById(conversation.getClosedById())
                .map(u -> displayName(u.getFullName(), u.getEmail()))
                .orElse(null);
    }

    private static boolean createdAtMissing(ChatConversation conversation) {
        return conversation.getCreatedAt() == null;
    }

    private static Long secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        long seconds = Duration.between(start, end).getSeconds();
        return Math.max(seconds, 0L);
    }

    private static String displayName(String fullName, String fallback) {
        return StringUtils.hasText(fullName) ? fullName : fallback;
    }
}
