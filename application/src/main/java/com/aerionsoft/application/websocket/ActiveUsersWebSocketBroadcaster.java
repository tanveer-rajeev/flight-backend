package com.aerionsoft.application.websocket;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.summery.ActiveUsersResponse;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ActiveUsersWebSocketBroadcaster {

    private final ActiveUserPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicInteger adminSubscriberCount = new AtomicInteger(0);

    public ActiveUsersWebSocketBroadcaster(ActiveUserPresenceService presenceService,
                                           SimpMessagingTemplate messagingTemplate) {
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    public void onAdminSubscribed() {
        adminSubscriberCount.incrementAndGet();
        publishActiveUsers();
    }

    public void onAdminUnsubscribed() {
        adminSubscriberCount.updateAndGet(count -> Math.max(0, count - 1));
    }

    public void publishActiveUsers() {
        if (adminSubscriberCount.get() <= 0) {
            return;
        }
        ActiveUsersResponse snapshot = presenceService.getActiveUsers();
        messagingTemplate.convertAndSend(
                WebSocketTopics.TOPIC_ADMIN_ACTIVE_USERS,
                BaseResponse.ok("Active Users (online now)", snapshot));
        log.debug("Broadcast active users to WebSocket ({} online)", snapshot.getTotalActiveUsers());
    }

    @Scheduled(fixedDelayString = "${presence.admin-broadcast-interval-ms:30000}")
    public void scheduledBroadcast() {
        if (adminSubscriberCount.get() > 0) {
            publishActiveUsers();
        }
    }
}
