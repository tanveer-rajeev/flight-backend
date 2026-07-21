package com.aerionsoft.application.dto.report;

import java.math.BigDecimal;

/**
 * Spring Data projection used to receive the per-business aggregated credit
 * summary from the native query in CreditLimitHistoryRepository.
 */
public interface CreditListProjection {
    Long       getBusinessId();
    String     getCompanyName();
    String     getCurrency();
    Double     getBalance();
    BigDecimal getCreditLimit();
}
