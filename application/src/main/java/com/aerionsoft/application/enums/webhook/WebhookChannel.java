package com.aerionsoft.application.enums.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WebhookChannel {
    DISCORD("DISCORD", "Discord"),
    SLACK("SLACK", "Slack"),
    MICROSOFT_TEAMS("MICROSOFT_TEAMS", "Microsoft Teams"),
    GENERIC_JSON("GENERIC_JSON", "Generic JSON");

    private final String value;
    private final String label;

    WebhookChannel(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static WebhookChannel fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Webhook channel is required");
        }
        for (WebhookChannel channel : values()) {
            if (channel.value.equalsIgnoreCase(value) || channel.name().equalsIgnoreCase(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Invalid webhook channel: " + value);
    }
}
