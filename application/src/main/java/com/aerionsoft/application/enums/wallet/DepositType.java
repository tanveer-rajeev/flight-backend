package com.aerionsoft.application.enums.wallet;

import java.util.Arrays;
import java.util.List;

public enum DepositType {
    CASH, CHEQUE, BANK_DEPOSIT, DEPOSIT, BANK_TRANSFER_OR_MFS, WITHDRAWAL, REFUND, STRIPE, INSTANT, PURCHASE, NGENIUS, SSL, BOOKING_STATUS_UPDATE_DEDUCTION, BOOKING_DEDUCTION, ADMIN_CHARGE;


    public static List<DepositType> getDepositTypes() {
        return Arrays.asList(
                CASH,
                CHEQUE,
                BANK_DEPOSIT,
                DEPOSIT,
                BANK_TRANSFER_OR_MFS,
                STRIPE,
                INSTANT,
                NGENIUS,
                SSL
        );
    }
}
