package com.aerionsoft.application.dto.admin.bank;

import com.aerionsoft.application.enums.common.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AdminChargeRequest {

    /** The user whose wallet will be debited. */
    @NotNull(message = "userId is required")
    private Long userId;

    /** Amount to deduct (must be > 0). */
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private Double amount;

    /** Currency of the charge. */
    @NotNull(message = "currency is required")
    private Currency currency;

    /** Mandatory reason / description for the charge (shown in transaction history). */
    @NotNull(message = "reason is required")
    private String reason;

    /**
     * If true and the user's wallet balance is insufficient, the shortfall will be
     * charged from the business credit limit instead of throwing an error.
     */
    private boolean chargeFromCredit = false;
}

