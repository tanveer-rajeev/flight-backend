package com.aerionsoft.notification.service;

import com.aerionsoft.notification.dto.request.NotificationTemplateRequest;
import com.aerionsoft.notification.dto.response.NotificationTemplateResponse;
import com.aerionsoft.notification.entity.NotificationTemplate;
import com.aerionsoft.notification.exception.InvalidNotificationTypeException;
import com.aerionsoft.notification.exception.NotificationTemplateNotFoundException;
import com.aerionsoft.notification.registry.NotificationTypeRegistry;
import com.aerionsoft.notification.repository.NotificationTemplateRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationTypeRegistry typeRegistry;

    public NotificationTemplateService(NotificationTemplateRepository templateRepository,
                                       NotificationTypeRegistry typeRegistry) {
        this.templateRepository = templateRepository;
        this.typeRegistry = typeRegistry;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> findAll() {
        return templateRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse findByTypeCodeAndLocale(String typeCode, String locale) {
        NotificationTemplate template = templateRepository.findByTypeCodeAndLocale(typeCode, locale)
                .orElseThrow(() -> NotificationTemplateNotFoundException.forTypeAndLocale(typeCode, locale));
        return toResponse(template);
    }

    @Transactional
    @CacheEvict(value = "notificationTemplates", allEntries = true)
    public NotificationTemplateResponse create(NotificationTemplateRequest request) {
        if (!typeRegistry.isValidCode(request.typeCode())) {
            throw new InvalidNotificationTypeException(request.typeCode());
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .typeCode(request.typeCode())
                .locale(request.locale())
                .titleTemplate(request.titleTemplate())
                .messageTemplate(request.messageTemplate())
                .defaultPriority(request.defaultPriority())
                .defaultReferenceType(request.defaultReferenceType())
                .build();

        return toResponse(templateRepository.save(template));
    }

    @Transactional
    @CacheEvict(value = "notificationTemplates", allEntries = true)
    public NotificationTemplateResponse update(Long id, NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> NotificationTemplateNotFoundException.forId(id));

        template.setTitleTemplate(request.titleTemplate());
        template.setMessageTemplate(request.messageTemplate());
        template.setDefaultPriority(request.defaultPriority());
        template.setDefaultReferenceType(request.defaultReferenceType());

        return toResponse(templateRepository.save(template));
    }

    @Transactional
    @CacheEvict(value = "notificationTemplates", allEntries = true)
    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw NotificationTemplateNotFoundException.forId(id);
        }
        templateRepository.deleteById(id);
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getId(),
                template.getTypeCode(),
                template.getLocale(),
                template.getTitleTemplate(),
                template.getMessageTemplate(),
                template.getDefaultPriority(),
                template.getDefaultReferenceType(),
                template.getUpdatedAt()
        );
    }
}