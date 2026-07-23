package com.aerionsoft.notification.repository;

import com.aerionsoft.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTypeCodeAndLocale(String typeCode, String locale);
}
