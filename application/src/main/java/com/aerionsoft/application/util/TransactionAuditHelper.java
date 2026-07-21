package com.aerionsoft.application.util;

import com.aerionsoft.application.entity.wallet.Transaction;

public final class TransactionAuditHelper {

    private TransactionAuditHelper() {
    }

    public static void touch(Transaction txn, String actor) {
        if (txn == null) {
            return;
        }
        txn.setUpdatedAt(UserDateTimeUtil.now());
        txn.setUpdatedBy(actor != null ? actor : "SYSTEM");
    }
}
