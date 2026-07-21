package com.aerionsoft.application.dto.payment;

import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.payment.PaymentStatus;
import com.aerionsoft.application.enums.wallet.PaymentProvider;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentResult(

        PaymentProvider provider,

        String providerPaymentId,

        String referenceId,

        PaymentStatus status,

        BigDecimal amount,

        Currency currency,

        String redirectUrl,

        String message

) {

    public boolean successful() {

        return status == PaymentStatus.CLOSED;
    }

    public boolean terminal() {

        return switch (status) {

            case CLOSED,
                 REJECTED,
                 EXPIRED -> true;

            default -> false;
        };
    }

}
