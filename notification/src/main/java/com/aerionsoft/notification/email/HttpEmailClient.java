package com.aerionsoft.notification.email;

import com.aerionsoft.notification.dto.request.EmailSendRequest;
import com.aerionsoft.notification.exception.NotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Slf4j
@Component
public class HttpEmailClient implements EmailClient {

    private final RestClient restClient;

    public HttpEmailClient(RestClient.Builder restClientBuilder,
                           @Value("${notification.email-service.base-url}") String baseUrl,
                           @Value("${notification.email-service.connect-timeout-ms:2000}") long connectTimeoutMs,
                           @Value("${notification.email-service.read-timeout-ms:3000}") long readTimeoutMs) {

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(
                ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .withReadTimeout(Duration.ofMillis(readTimeoutMs))
        );

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void sendEmail(String toAddress, String subject, String htmlBody) {
        try {
            restClient.post()
                    .uri("/api/v1/emails/send")
                    .body(new EmailSendRequest(toAddress, subject, htmlBody))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Email service call failed for recipient={}", toAddress, e);
            throw new NotificationException("Failed to send email via Email service", e);
        }
    }
}
