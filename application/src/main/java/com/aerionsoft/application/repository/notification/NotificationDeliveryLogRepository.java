package com.aerionsoft.application.repository.notification;

import com.aerionsoft.application.entity.NotificationDeliveryLog;
import com.aerionsoft.application.enums.notification.DeliveryChannel;
import com.aerionsoft.application.enums.notification.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, Long> {

    List<NotificationDeliveryLog> findByNotificationId(Long notificationId);

    List<NotificationDeliveryLog> findByNotificationIdAndDeliveryChannel(Long notificationId, DeliveryChannel channel);

    List<NotificationDeliveryLog> findByStatus(DeliveryStatus status);

    @Query("SELECT ndl FROM NotificationDeliveryLog ndl WHERE ndl.status = :status AND ndl.createdAt < :cutoffDate")
    List<NotificationDeliveryLog> findPendingDeliveriesOlderThan(
            @Param("status") DeliveryStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );

    long countByDeliveryChannelAndStatus(DeliveryChannel channel, DeliveryStatus status);
}

