package com.aerionsoft.application.exception;

public class TabbyPaymentNotFoundException extends RuntimeException {

    private final String tabbyPaymentId;

    public TabbyPaymentNotFoundException(String tabbyPaymentId) {
        super("No local TabbyPayment found for Tabby payment id: " + tabbyPaymentId);
        this.tabbyPaymentId = tabbyPaymentId;
    }

    public String tabbyPaymentId() {
        return tabbyPaymentId;
    }
}
