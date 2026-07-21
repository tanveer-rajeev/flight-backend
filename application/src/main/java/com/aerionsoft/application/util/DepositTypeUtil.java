package com.aerionsoft.application.util;

import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.enums.wallet.TransactionStatus;

public class DepositTypeUtil {

    public static TransactionStatus determineStatus(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("DepositType string is null or empty");
        }

        DepositType depositType;
        try {
            depositType = DepositType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown DepositType: " + type);
        }

        return switch (depositType) {
            case WITHDRAWAL, PURCHASE, BOOKING_DEDUCTION, BOOKING_STATUS_UPDATE_DEDUCTION, ADMIN_CHARGE ->
                TransactionStatus.DEBIT;
            case REFUND, STRIPE, INSTANT, CASH, CHEQUE, NGENIUS, SSL, BANK_DEPOSIT, BANK_TRANSFER_OR_MFS, DEPOSIT ->
                TransactionStatus.CREDIT;
            default -> throw new IllegalStateException("Unhandled DepositType: " + depositType);
        };
    }

}
