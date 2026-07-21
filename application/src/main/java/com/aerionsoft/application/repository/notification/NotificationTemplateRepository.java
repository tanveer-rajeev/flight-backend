package com.aerionsoft.application.repository.notification;

import com.aerionsoft.application.entity.NotificationTemplate;
import com.aerionsoft.application.enums.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateCode(String templateCode);

    boolean existsByTemplateCode(String templateCode);

    List<NotificationTemplate> findAllByOrderByCreatedAtDesc();

    List<NotificationTemplate> findByType(NotificationType type);

    List<NotificationTemplate> findByIsActive(Boolean isActive);

    List<NotificationTemplate> findByIsActiveOrderByCreatedAtDesc(Boolean isActive);

    Optional<NotificationTemplate> findByTemplateCodeAndIsActive(String templateCode, Boolean isActive);
}
