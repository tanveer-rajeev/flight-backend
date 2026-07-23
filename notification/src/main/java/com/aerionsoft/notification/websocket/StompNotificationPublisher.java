package com.aerionsoft.notification.websocket;

import com.aerionsoft.notification.dto.websocket.NotificationSocketMessage;
import com.aerionsoft.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StompNotificationPublisher implements NotificationWebSocketPublisher {

    private static final String USER_DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public StompNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(Notification notification) {
        NotificationSocketMessage payload = new NotificationSocketMessage(
                notification.getId(),
                notification.getTypeCode(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getActionUrl(),
                notification.getActionLabel(),
                notification.getCreatedAt()
        );

        messagingTemplate.convertAndSendToUser(
                notification.getUserId().toString(),
                USER_DESTINATION,
                payload
        );
    }
}