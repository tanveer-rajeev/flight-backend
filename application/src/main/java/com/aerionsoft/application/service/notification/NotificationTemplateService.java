package com.aerionsoft.application.service.notification;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.notification.NotificationTemplateRequest;
import com.aerionsoft.application.dto.notification.NotificationTemplateResponse;
import com.aerionsoft.application.entity.NotificationTemplate;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.repository.notification.NotificationTemplateRepository;
import com.aerionsoft.application.util.TimestampMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TimestampMapper timestampMapper;

    public List<NotificationTemplateResponse> getAllTemplates(Boolean active) {
        List<NotificationTemplate> templates = active == null
                ? templateRepository.findAllByOrderByCreatedAtDesc()
                : templateRepository.findByIsActiveOrderByCreatedAtDesc(active);

        return templates.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public NotificationTemplateResponse getTemplateById(Long id) {
        return templateRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Notification template"));
    }

    public NotificationTemplateResponse getTemplateByCode(String templateCode) {
        return templateRepository.findByTemplateCode(normalizeCode(templateCode))
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Notification template", templateCode));
    }

    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        String templateCode = normalizeCode(request.getTemplateCode());
        if (templateRepository.existsByTemplateCode(templateCode)) {
            throw new IllegalArgumentException("Notification template already exists: " + templateCode);
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .templateCode(templateCode)
                .type(request.getType())
                .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
                .titleTemplate(request.getTitleTemplate())
                .messageTemplate(request.getMessageTemplate())
                .actionUrlTemplate(request.getActionUrlTemplate())
                .actionLabel(request.getActionLabel())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToResponse(templateRepository.save(template));
    }

    @Transactional
    public NotificationTemplateResponse updateTemplate(Long id, NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification template"));

        String templateCode = normalizeCode(request.getTemplateCode());
        templateRepository.findByTemplateCode(templateCode)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Notification template already exists: " + templateCode);
                });

        template.setTemplateCode(templateCode);
        template.setType(request.getType());
        template.setPriority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM);
        template.setTitleTemplate(request.getTitleTemplate());
        template.setMessageTemplate(request.getMessageTemplate());
        template.setActionUrlTemplate(request.getActionUrlTemplate());
        template.setActionLabel(request.getActionLabel());
        template.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        return mapToResponse(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification template");
        }
        templateRepository.deleteById(id);
    }

    private String normalizeCode(String templateCode) {
        return templateCode.trim().toUpperCase(Locale.ROOT);
    }

    private NotificationTemplateResponse mapToResponse(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
                .id(template.getId())
                .templateCode(template.getTemplateCode())
                .type(template.getType())
                .priority(template.getPriority())
                .titleTemplate(template.getTitleTemplate())
                .messageTemplate(template.getMessageTemplate())
                .actionUrlTemplate(template.getActionUrlTemplate())
                .actionLabel(template.getActionLabel())
                .isActive(template.getIsActive())
                .createdAt(timestampMapper.toRequestUserTime(template.getCreatedAt(), template.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(template.getUpdatedAt(), template.getUpdatedTimeOffset() != null ? template.getUpdatedTimeOffset() : template.getCreatedTimeOffset()))
                .build();
    }
}
