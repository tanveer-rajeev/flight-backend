package com.aerionsoft.application.service.webhook;

import com.aerionsoft.application.dto.webhook.WebhookAlertConfigRequest;
import com.aerionsoft.application.dto.webhook.WebhookAlertConfigResponse;
import com.aerionsoft.application.dto.webhook.WebhookAlertMetaResponse;
import com.aerionsoft.application.dto.webhook.WebhookAlertOptionDTO;
import com.aerionsoft.application.entity.webhook.WebhookAlertConfig;
import com.aerionsoft.application.enums.webhook.WebhookAlertType;
import com.aerionsoft.application.enums.webhook.WebhookChannel;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.repository.webhook.WebhookAlertConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookAlertConfigServiceImpl implements WebhookAlertConfigService {

    private final WebhookAlertConfigRepository repository;
    private final WebhookOutboundSender outboundSender;

    @Override
    @Transactional
    public WebhookAlertConfigResponse create(WebhookAlertConfigRequest request, Long adminUserId) {
        validateWebhookUrl(request.getWebhookUrl());
        WebhookAlertConfig entity = WebhookAlertConfig.builder()
                .name(request.getName().trim())
                .alertType(request.getAlertType())
                .channel(request.getChannel())
                .webhookUrl(request.getWebhookUrl().trim())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(adminUserId)
                .updatedBy(adminUserId)
                .build();
        return mapToResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public WebhookAlertConfigResponse update(Long id, WebhookAlertConfigRequest request, Long adminUserId) {
        validateWebhookUrl(request.getWebhookUrl());
        WebhookAlertConfig entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook alert config", id));
        entity.setName(request.getName().trim());
        entity.setAlertType(request.getAlertType());
        entity.setChannel(request.getChannel());
        entity.setWebhookUrl(request.getWebhookUrl().trim());
        entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
        entity.setUpdatedBy(adminUserId);
        return mapToResponse(repository.save(entity));
    }

    @Override
    public WebhookAlertConfigResponse getById(Long id) {
        return repository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook alert config", id));
    }

    @Override
    public List<WebhookAlertConfigResponse> getAll(Boolean activeOnly) {
        List<WebhookAlertConfig> configs = Boolean.TRUE.equals(activeOnly)
                ? repository.findByIsActiveTrueOrderByAlertTypeAscIdAsc()
                : repository.findAllByOrderByAlertTypeAscIdAsc();
        return configs.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Webhook alert config", id);
        }
        repository.deleteById(id);
    }

    @Override
    public WebhookAlertMetaResponse getMeta() {
        List<WebhookAlertOptionDTO> alertTypes = Arrays.stream(WebhookAlertType.values())
                .map(type -> WebhookAlertOptionDTO.builder()
                        .value(type.getValue())
                        .label(type.getLabel())
                        .build())
                .toList();
        List<WebhookAlertOptionDTO> channels = Arrays.stream(WebhookChannel.values())
                .map(channel -> WebhookAlertOptionDTO.builder()
                        .value(channel.getValue())
                        .label(channel.getLabel())
                        .build())
                .toList();
        return WebhookAlertMetaResponse.builder()
                .alertTypes(alertTypes)
                .channels(channels)
                .build();
    }

    @Override
    public void sendTestAlert(Long id) {
        WebhookAlertConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook alert config", id));
        outboundSender.send(config, WebhookChannelPayloadBuilder.buildTestMessage(config));
    }

    private WebhookAlertConfigResponse mapToResponse(WebhookAlertConfig entity) {
        return WebhookAlertConfigResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .alertType(entity.getAlertType())
                .alertTypeLabel(entity.getAlertType().getLabel())
                .channel(entity.getChannel())
                .channelLabel(entity.getChannel().getLabel())
                .webhookUrl(entity.getWebhookUrl())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void validateWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Webhook URL is required");
        }
        try {
            URI uri = URI.create(webhookUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Webhook URL must use http or https");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Webhook URL host is invalid");
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Webhook URL is invalid");
        }
    }
}
