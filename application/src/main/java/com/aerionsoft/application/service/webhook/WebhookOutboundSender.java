package com.aerionsoft.application.service.webhook;

import com.aerionsoft.application.dto.webhook.WebhookAlertMessage;
import com.aerionsoft.application.entity.webhook.WebhookAlertConfig;
import com.aerionsoft.application.enums.webhook.WebhookChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WebhookOutboundSender {

    private final RestTemplate restTemplate = new RestTemplate();

    public void send(WebhookAlertConfig config, WebhookAlertMessage message) {
        try {
            Object payload = buildPayload(config.getChannel(), message);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(config.getWebhookUrl(), entity, String.class);
            log.info("Webhook alert sent. configId={}, alertType={}, channel={}, status={}",
                    config.getId(), config.getAlertType(), config.getChannel(), response.getStatusCode().value());
        } catch (RestClientException ex) {
            log.warn("Failed to send webhook alert. configId={}, alertType={}, channel={}, error={}",
                    config.getId(), config.getAlertType(), config.getChannel(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected webhook alert failure. configId={}, error={}", config.getId(), ex.getMessage());
        }
    }

    private Object buildPayload(WebhookChannel channel, WebhookAlertMessage message) {
        return switch (channel) {
            case DISCORD -> buildDiscordPayload(message);
            case SLACK -> buildSlackPayload(message);
            case MICROSOFT_TEAMS -> buildTeamsPayload(message);
            case GENERIC_JSON -> buildGenericPayload(message);
        };
    }

    private Map<String, Object> buildDiscordPayload(WebhookAlertMessage message) {
        Map<String, Object> embed = new HashMap<>();
        embed.put("title", message.getTitle());
        embed.put("description", trimToDiscordLimit(message.getBody(), 4096));
        embed.put("color", 15158332);

        if (message.getFields() != null && !message.getFields().isEmpty()) {
            List<Map<String, Object>> fields = new ArrayList<>();
            for (Map.Entry<String, String> entry : message.getFields().entrySet()) {
                Map<String, Object> field = new HashMap<>();
                field.put("name", entry.getKey());
                field.put("value", trimToDiscordLimit(entry.getValue(), 1024));
                field.put("inline", false);
                fields.add(field);
            }
            embed.put("fields", fields);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "@here **" + message.getTitle() + "**");
        payload.put("embeds", List.of(embed));
        return payload;
    }

    private Map<String, String> buildSlackPayload(WebhookAlertMessage message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("text", "*" + message.getTitle() + "*\n" + message.getBody());
        return payload;
    }

    private Map<String, Object> buildTeamsPayload(WebhookAlertMessage message) {
        List<Map<String, String>> facts = new ArrayList<>();
        if (message.getFields() != null) {
            for (Map.Entry<String, String> entry : message.getFields().entrySet()) {
                Map<String, String> fact = new HashMap<>();
                fact.put("name", entry.getKey());
                fact.put("value", entry.getValue());
                facts.add(fact);
            }
        }

        Map<String, Object> section = new HashMap<>();
        section.put("activityTitle", message.getTitle());
        section.put("activitySubtitle", "TufanTrip webhook alert");
        section.put("facts", facts);
        section.put("text", message.getBody());

        Map<String, Object> payload = new HashMap<>();
        payload.put("@type", "MessageCard");
        payload.put("@context", "http://schema.org/extensions");
        payload.put("summary", message.getTitle());
        payload.put("themeColor", "D13438");
        payload.put("sections", List.of(section));
        return payload;
    }

    private Map<String, Object> buildGenericPayload(WebhookAlertMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", message.getTitle());
        payload.put("message", message.getBody());
        payload.put("fields", message.getFields());
        return payload;
    }

    private String trimToDiscordLimit(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
