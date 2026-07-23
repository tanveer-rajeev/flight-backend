package com.aerionsoft.application.websocket;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.audit.ActivityFeedEvent;
import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.service.audit.ActivityFeedFilter;
import com.aerionsoft.application.service.audit.ActivityFeedMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ActivityFeedBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final ActivityFeedMapper activityFeedMapper;
    private final AtomicInteger adminSubscriberCount = new AtomicInteger(0);

    public ActivityFeedBroadcaster(
            SimpMessagingTemplate messagingTemplate,
            ActivityFeedMapper activityFeedMapper) {
        this.messagingTemplate = messagingTemplate;
        this.activityFeedMapper = activityFeedMapper;
    }

    public void onAdminSubscribed() {
        adminSubscriberCount.incrementAndGet();
    }

    public void onAdminUnsubscribed() {
        adminSubscriberCount.updateAndGet(count -> Math.max(0, count - 1));
    }

    public void onActivityLogged(ActivityLog activityLog) {
        if (!ActivityFeedFilter.isFeedEligible(activityLog)) {
            return;
        }
        if (adminSubscriberCount.get() <= 0) {
            return;
        }

        ActivityFeedEvent event = activityFeedMapper.toFeedEvent(activityLog);
        messagingTemplate.convertAndSend(
                WebSocketTopics.TOPIC_ADMIN_ACTIVITY_FEED,
                BaseResponse.ok("Activity feed event", event));
        log.debug("Broadcast activity feed event id={} type={}", activityLog.getId(), activityLog.getEventType());
    }
}
