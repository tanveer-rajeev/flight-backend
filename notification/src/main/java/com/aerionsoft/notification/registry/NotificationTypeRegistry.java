package com.aerionsoft.notification.registry;

import com.aerionsoft.notification.entity.NotificationType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class NotificationTypeRegistry {

    private static final String DOMAIN_ENUM_PACKAGE = "com.aerionsoft.notification.enums.type";

    private final Set<String> validCodes = new HashSet<>();

    @PostConstruct
    void scanAndRegister() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(NotificationType.class));

        scanner.findCandidateComponents(DOMAIN_ENUM_PACKAGE).forEach(beanDef -> {
            try {
                Class<?> clazz = Class.forName(beanDef.getBeanClassName());
                if (clazz.isEnum() && NotificationType.class.isAssignableFrom(clazz)) {
                    for (Object constant : clazz.getEnumConstants()) {
                        validCodes.add(((NotificationType) constant).getCode());
                    }
                }
            } catch (ClassNotFoundException e) {
                log.warn("Could not load candidate notification type class: {}", beanDef.getBeanClassName(), e);
            }
        });

        log.info("Registered {} valid notification type codes", validCodes.size());
    }

    public boolean isValidCode(String typeCode) {
        return validCodes.contains(typeCode);
    }

    public Set<String> getAllValidCodes() {
        return Collections.unmodifiableSet(validCodes);
    }
}