package com.aerionsoft.application.dto.admin.bank;

import com.aerionsoft.application.enums.common.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AdminDepositRequest {

    /** The mother/parent user whose wallet will be credited. */
    @NotNull(message = "motherUserId is required")
    private Long motherUserId;

    /** Proof of payment image URL. */
    private String imageUrl;

    /** Amount to credit (must be > 0). */
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private Double amount;

    /** Currency of the deposit. */
    @NotNull(message = "currency is required")
    private Currency currency;

    /** Optional remarks / description for the deposit. */
    private String remarks;

    /** When set, credits the company bank ledger in addition to the agent wallet. */
    private Long depositBankId;
}
