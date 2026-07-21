package com.aerionsoft.application.service.payment;

import com.aerionsoft.application.enums.wallet.PaymentProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayFactory {
    private final Map<PaymentProvider, PaymentGateway> gateways;

    public PaymentGatewayFactory(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        PaymentGateway::provider,
                        Function.identity()));
    }

    public PaymentGateway getGateway(PaymentProvider provider) {
        PaymentGateway gateway = gateways.get(provider);

        if (gateway == null) {
            throw new IllegalArgumentException(
                    "Unsupported payment provider: " + provider);
        }

        return gateway;
    }
}
