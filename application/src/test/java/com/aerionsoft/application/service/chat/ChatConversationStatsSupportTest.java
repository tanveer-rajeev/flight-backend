package com.aerionsoft.application.service.chat;

import com.aerionsoft.application.dto.chat.ChatConversationStatsDTO;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.chat.ChatConversation;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import com.aerionsoft.application.repository.chat.ChatMessageRepository;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatConversationStatsSupportTest {

    @Mock private ChatMessageRepository messageRepository;
    @Mock private AdminUserRepository adminUserRepository;
    @Mock private UserRepository userRepository;

    private ChatConversationStatsSupport support;

    @BeforeEach
    void setUp() {
        support = new ChatConversationStatsSupport(messageRepository, adminUserRepository, userRepository);
    }

    @Test
    void build_closedConversation_includesDurationAndCounts() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime claimed = LocalDateTime.of(2026, 1, 1, 10, 5);
        LocalDateTime closed = LocalDateTime.of(2026, 1, 1, 10, 20);

        User user = User.builder().id(1L).email("user@test.com").fullName("Agency User").password("x").build();
        ChatConversation conversation = ChatConversation.builder()
                .id(10L)
                .user(user)
                .status(ChatConversationStatus.CLOSED)
                .createdAt(created)
                .claimedAt(claimed)
                .closedAt(closed)
                .closedByType(ChatSenderType.ADMIN)
                .closedById(99L)
                .build();

        when(messageRepository.countGroupedByConversationIds(anyCollection())).thenReturn(List.<Object[]>of(
                new Object[] {10L, ChatSenderType.USER, false, 3L},
                new Object[] {10L, ChatSenderType.ADMIN, false, 2L},
                new Object[] {10L, ChatSenderType.ADMIN, true, 1L}
        ));
        when(messageRepository.findFirstAdminReplyAtByConversationIds(anyCollection())).thenReturn(List.<Object[]>of(
                new Object[] {10L, claimed.plusMinutes(1)}
        ));
        when(adminUserRepository.findById(99L)).thenReturn(Optional.of(
                AdminUser.builder().id(99L).email("admin@test.com").fullName("Ops Admin").password("x").build()));

        ChatConversationStatsSupport.StatsContext context = support.loadContext(List.of(10L));
        ChatConversationStatsDTO stats = support.build(conversation, true, context);

        assertThat(stats.getDurationSeconds()).isEqualTo(20 * 60L);
        assertThat(stats.getWaitTimeSeconds()).isEqualTo(5 * 60L);
        assertThat(stats.getActiveDurationSeconds()).isEqualTo(15 * 60L);
        assertThat(stats.getUserMessageCount()).isEqualTo(3L);
        assertThat(stats.getAdminMessageCount()).isEqualTo(2L);
        assertThat(stats.getInternalNoteCount()).isEqualTo(1L);
        assertThat(stats.getTotalMessageCount()).isEqualTo(5L);
        assertThat(stats.getClosedByName()).isEqualTo("Ops Admin");
        assertThat(stats.getFirstAdminReplyAt()).isEqualTo(claimed.plusMinutes(1));
    }

    @Test
    void build_openConversation_waitTimeGrowsUntilClaim() {
        LocalDateTime created = LocalDateTime.now().minusMinutes(12);
        ChatConversation conversation = ChatConversation.builder()
                .id(11L)
                .status(ChatConversationStatus.OPEN)
                .createdAt(created)
                .build();

        ChatConversationStatsDTO stats = support.build(conversation, true, new ChatConversationStatsSupport.StatsContext(Map.of(), Map.of()));

        assertThat(stats.getWaitTimeSeconds()).isGreaterThanOrEqualTo(12 * 60L - 2);
        assertThat(stats.getActiveDurationSeconds()).isNull();
    }
}
