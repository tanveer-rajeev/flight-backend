package com.aerionsoft.application.util;

import com.aerionsoft.application.enums.payment.PaymentStatus;
import com.aerionsoft.application.enums.wallet.PaymentProvider;
import org.springframework.web.util.UriComponentsBuilder;

public class PaymentCallBackUrlBuilder {
    public static String build(String baseUrl, PaymentStatus status, PaymentProvider provider) {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("status", status.name().toLowerCase())
                .queryParam("provider", provider.name().toLowerCase())
                .build()
                .toUriString();
    }
}
