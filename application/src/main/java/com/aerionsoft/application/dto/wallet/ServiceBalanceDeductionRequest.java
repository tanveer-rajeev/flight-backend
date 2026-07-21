package com.aerionsoft.application.dto.wallet;

import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.ServiceDeductionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ServiceBalanceDeductionRequest {

    @NotNull(message = "serviceType is required (VISA, TOUR, or HOTEL)")
    private ServiceDeductionType serviceType;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private Double amount;

    @NotNull(message = "currency is required")
    private Currency currency;

    /** Visa application id, tour application id, or hotel booking id. */
    private Long sourceId;

    private String description;

    /** Admin only: deduct from this user's wallet instead of the authenticated user. */
    private Long userId;
}
