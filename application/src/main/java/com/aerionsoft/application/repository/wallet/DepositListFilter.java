package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;

import java.time.Instant;
import java.util.List;

public record DepositListFilter(
        boolean admin,
        Long userId,
        Long actingUserId,
        List<DepositStatus> statuses,
        List<DepositType> types,
        Currency currency,
        Instant createdFromInstant,
        Instant createdToInstantExclusive
) {
    public boolean hasUserScope() {
        return userId != null;
    }

    public boolean hasActingScope() {
        return actingUserId != null;
    }
}
