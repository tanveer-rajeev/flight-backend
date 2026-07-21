package com.aerionsoft.application.repository.notification;

import com.aerionsoft.application.entity.Notification;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationStatus;
import com.aerionsoft.application.enums.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find notifications by user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Find by user and status
    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status, Pageable pageable);

    // Find by user and type
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);

    // Find by user and priority
    Page<Notification> findByUserIdAndPriorityOrderByCreatedAtDesc(Long userId, NotificationPriority priority, Pageable pageable);

    // Count unread notifications
    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    // Count by user and priority
    long countByUserIdAndPriority(Long userId, NotificationPriority priority);

    // Find recent unread notifications (limit to top N)
    List<Notification> findTop10ByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);

    // Find by reference
    List<Notification> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    // Complex filter query
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND (:type IS NULL OR n.type = :type) " +
           "AND (:status IS NULL OR n.status = :status) " +
           "AND (:priority IS NULL OR n.priority = :priority) " +
           "AND (:fromDate IS NULL OR n.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR n.createdAt <= :toDate) " +
           "AND (:referenceType IS NULL OR n.referenceType = :referenceType) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByFilters(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("status") NotificationStatus status,
            @Param("priority") NotificationPriority priority,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("referenceType") String referenceType,
            Pageable pageable
    );

    // Mark as read
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    // Mark all as read
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    // Mark as archived
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'ARCHIVED', n.archivedAt = :archivedAt WHERE n.id = :id AND n.userId = :userId")
    int markAsArchived(@Param("id") Long id, @Param("userId") Long userId, @Param("archivedAt") LocalDateTime archivedAt);

    // Delete old archived notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status = 'ARCHIVED' AND n.archivedAt < :cutoffDate")
    int deleteOldArchivedNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Delete expired notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") LocalDateTime now);

    // Find by user and business
    Page<Notification> findByUserIdAndBusinessIdOrderByCreatedAtDesc(Long userId, Long businessId, Pageable pageable);

    // Get notification by ID and user ID (for authorization)
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);

    // Get summary counts
    @Query("SELECT new map(" +
           "COUNT(n) as total, " +
           "SUM(CASE WHEN n.status = 'UNREAD' THEN 1 ELSE 0 END) as unread, " +
           "SUM(CASE WHEN n.status = 'READ' THEN 1 ELSE 0 END) as read, " +
           "SUM(CASE WHEN n.status = 'ARCHIVED' THEN 1 ELSE 0 END) as archived, " +
           "SUM(CASE WHEN n.priority = 'URGENT' THEN 1 ELSE 0 END) as urgent, " +
           "SUM(CASE WHEN n.priority = 'HIGH' THEN 1 ELSE 0 END) as high) " +
           "FROM Notification n WHERE n.userId = :userId")
    Object getSummaryByUserId(@Param("userId") Long userId);
}
