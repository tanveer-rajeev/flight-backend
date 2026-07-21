package com.aerionsoft.application.enums.wallet;

public enum ServiceDeductionType {
    VISA,
    TOUR,
    HOTEL;

    public TransactionSourceType toTransactionSourceType() {
        return TransactionSourceType.valueOf(name());
    }
}
