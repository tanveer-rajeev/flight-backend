package com.aerionsoft.application.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "tabby")
public record TabbyProperties(

        @NotBlank
        String baseUrl,

        @NotBlank
        String publicKey,

        @NotBlank
        String secretKey,

        @NotBlank
        String merchantCode,

        @NotBlank
        String merchantName,

        @NotNull
        Urls merchant_urls,

        Duration connectTimeout,
        Duration readTimeout,
        Boolean logRequests
) {
    public TabbyProperties {
        connectTimeout = connectTimeout != null ? connectTimeout : Duration.ofSeconds(5);
        readTimeout = readTimeout != null ? readTimeout : Duration.ofSeconds(10);
        logRequests = logRequests != null ? logRequests : false;
    }

    public record Urls(
            @NotBlank String success,
            @NotBlank String cancel,
            @NotBlank String failure
    ) {}
}