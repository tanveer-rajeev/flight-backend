package com.aerionsoft.application.dto.wallet;

import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.ServiceDeductionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceBalanceDeductionResponse {

    private Long transactionId;
    private Long depositId;
    private String reference;
    private ServiceDeductionType serviceType;
    private Long sourceId;
    private Double amount;
    private Currency currency;
    private Double balanceBefore;
    private Double balanceAfter;
    private Double availableBalanceBefore;
}
