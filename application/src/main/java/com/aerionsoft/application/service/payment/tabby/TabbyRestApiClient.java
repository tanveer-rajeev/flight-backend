package com.aerionsoft.application.service.payment.tabby;

import com.aerionsoft.application.dto.payment.tabby.*;
import com.aerionsoft.application.exception.TabbyApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class TabbyRestApiClient implements TabbyApiClient {

    private final RestClient restClient;

    public TabbyRestApiClient(@Qualifier("tabbyRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public TabbyCheckoutSessionResponse createCheckoutSession(TabbyCheckoutSessionRequest request) {
        return restClient.post()
                .uri("/v2/checkout")
                .body(request)
                .retrieve()
                .body(TabbyCheckoutSessionResponse.class);
    }

    @Override
    @Retryable(
            retryFor = TabbyApiException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public TabbyPaymentDetailsResponse getPayment(String paymentId) {
        return restClient.get()
                .uri("/v2/payments/{id}", paymentId)
                .retrieve()
                .body(TabbyPaymentDetailsResponse.class);
    }

    @Override
    public TabbyPaymentDetailsResponse capturePayment(String paymentId, CapturePaymentRequest request) {
        return  restClient.post()
                .uri("/v1/payments/{id}/captures", paymentId)
                .body(request)
                .retrieve()
                .body(TabbyPaymentDetailsResponse.class);
    }

    @Override
    public void refundPayment(String paymentId, RefundPaymentRequest request) {
        restClient.post()
                .uri("/v1/payments/{id}/refunds", paymentId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void closePayment(String paymentId) {
        restClient.post()
                .uri("/v1/payments/{id}/close", paymentId)
                .retrieve()
                .toBodilessEntity();
    }
}