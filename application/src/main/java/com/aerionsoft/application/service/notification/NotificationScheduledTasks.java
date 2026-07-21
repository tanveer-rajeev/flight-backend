package com.aerionsoft.application.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for notification system maintenance
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduledTasks {

    private final NotificationService notificationService;

    /**
     * Cleanup expired and old archived notifications
     * Runs every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupNotifications() {
        log.info("Starting scheduled notification cleanup");
        try {
            notificationService.cleanupNotifications();
            log.info("Notification cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during notification cleanup", e);
        }
    }
}

