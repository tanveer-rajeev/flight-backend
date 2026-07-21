package com.aerionsoft.application.service.payment;

import com.aerionsoft.application.dto.payment.CheckoutResult;
import com.aerionsoft.application.dto.payment.PaymentResult;
import com.aerionsoft.application.dto.payment.tabby.TabbyCheckoutCommand;
import com.aerionsoft.application.enums.wallet.PaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentFacadeServiceImpl implements PaymentFacadeService {

    private final PaymentGatewayFactory gatewayFactory;

    @Override
    public CheckoutResult checkout(PaymentProvider provider, TabbyCheckoutCommand command, Long userId) {

        PaymentGateway gateway = gatewayFactory.getGateway(provider);

        return gateway.checkout(command,userId);
    }

    @Override
    public PaymentResult confirm(PaymentProvider provider, String paymentId) {

        PaymentGateway gateway = gatewayFactory.getGateway(provider);

        return gateway.confirm(paymentId);
    }
}
