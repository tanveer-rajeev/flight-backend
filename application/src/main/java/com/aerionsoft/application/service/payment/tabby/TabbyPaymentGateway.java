package com.aerionsoft.application.service.payment.tabby;

import com.aerionsoft.application.dto.payment.CheckoutResult;
import com.aerionsoft.application.dto.payment.PaymentResult;
import com.aerionsoft.application.dto.payment.tabby.TabbyCheckoutCommand;
import com.aerionsoft.application.dto.payment.tabby.TabbyCheckoutResult;
import com.aerionsoft.application.entity.paymentGateway.TabbyPayment;
import com.aerionsoft.application.enums.wallet.PaymentProvider;
import com.aerionsoft.application.service.payment.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TabbyPaymentGateway implements PaymentGateway {

    private final TabbyPaymentService tabbyPaymentService;

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.TABBY;
    }

    @Override
    public CheckoutResult checkout(TabbyCheckoutCommand command, Long userId) {

        TabbyCheckoutCommand tabbyCommand = new TabbyCheckoutCommand(
                userId,
                command.referenceId(),
                command.amount(),
                command.currency(),
                command.buyerName(),
                command.buyerEmail(),
                command.buyerPhone(),
                command.paymentType()
        );

        TabbyCheckoutResult result = tabbyPaymentService.initiateCheckout(tabbyCommand);
        return new CheckoutResult(result.paymentId(), result.status(), result.redirectUrl());
    }

    @Override
    public PaymentResult confirm(String paymentId) {

        TabbyPayment payment =
                tabbyPaymentService.confirmPayment(paymentId);

        return PaymentResult.builder()
                .provider(PaymentProvider.TABBY)
                .providerPaymentId(payment.getPaymentId())
                .referenceId(payment.getOrderId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .build();
    }

}
