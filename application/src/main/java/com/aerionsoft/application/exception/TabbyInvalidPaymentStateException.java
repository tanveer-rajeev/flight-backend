package com.aerionsoft.application.exception;

import com.aerionsoft.application.enums.payment.PaymentStatus;

public class TabbyInvalidPaymentStateException extends RuntimeException {

    private final String tabbyPaymentId;
    private final PaymentStatus actualStatus;
    private final PaymentStatus expectedStatus;

    public TabbyInvalidPaymentStateException(String tabbyPaymentId,
                                             PaymentStatus actualStatus,
                                             PaymentStatus expectedStatus) {
        super("Cannot transition payment %s from %s, expected %s"
                .formatted(tabbyPaymentId, actualStatus, expectedStatus));
        this.tabbyPaymentId = tabbyPaymentId;
        this.actualStatus = actualStatus;
        this.expectedStatus = expectedStatus;
    }

    public String tabbyPaymentId() { return tabbyPaymentId; }
    public PaymentStatus actualStatus() { return actualStatus; }
    public PaymentStatus expectedStatus() { return expectedStatus; }
}
