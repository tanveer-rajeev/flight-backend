package com.aerionsoft.application.websocket;

import com.aerionsoft.application.service.access.PermissionService;
import com.aerionsoft.application.service.user.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorChatTest {

    @Mock private WebSocketAuthService webSocketAuthService;
    @Mock private PermissionService permissionService;

    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(webSocketAuthService, permissionService);
    }

    @Test
    void subscribeInbox_rejectsNonAdmin() {
        CustomUserDetails user = new CustomUserDetails(
                1L, "user@example.com", "x", true, true, List.of(), "user");
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(WebSocketTopics.TOPIC_ADMIN_CHAT_INBOX);
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);

        when(webSocketAuthService.isAdminUser(auth)).thenReturn(false);

        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Admin token required");
    }

    @Test
    void subscribeInbox_allowsAnyAdmin() {
        CustomUserDetails admin = new CustomUserDetails(
                1L, "admin@example.com", "x", true, true, List.of(), "admin");
        Authentication auth = new UsernamePasswordAuthenticationToken(admin, null, List.of());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(WebSocketTopics.TOPIC_ADMIN_CHAT_INBOX);
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);

        when(webSocketAuthService.isAdminUser(any())).thenReturn(true);

        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatCode(() -> interceptor.preSend(message, null)).doesNotThrowAnyException();
    }
}
