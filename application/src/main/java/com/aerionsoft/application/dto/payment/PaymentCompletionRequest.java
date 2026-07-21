package com.aerionsoft.application.dto.payment;

import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.DepositType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentCompletionRequest {

    private Long userId;

    private Double amount;

    private Currency currency;

    private DepositType depositType;

    private String paymentProvider;

    private String paymentTransactionId;

    private String description;

    private Long createdBy;

    private String reference;
}