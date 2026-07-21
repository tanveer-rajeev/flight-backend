package com.aerionsoft.application.util;

import com.aerionsoft.application.entity.wallet.DepositBank;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import org.hibernate.Hibernate;

public final class DepositBankMapper {

    private DepositBankMapper() {}

    public static DepositBank resolve(WalletDeposit deposit) {
        if (deposit == null) {
            return null;
        }
        DepositBank bank = deposit.getDepositBank();
        if (bank == null || !Hibernate.isInitialized(bank)) {
            return null;
        }
        return (DepositBank) Hibernate.unproxy(bank);
    }
}
