package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.DepositReportDTO;
import com.aerionsoft.application.enums.wallet.DepositType;

import java.time.LocalDate;

public interface DepositReportService {

    /**
     * Returns a paginated deposit list for the supported types:
     * CASH, CHEQUE, BANK_DEPOSIT, DEPOSIT, BANK_TRANSFER_OR_MFS,
     * STRIPE, INSTANT, PURCHASE, NGENIUS, SSL.
     *
     * @param type     optional single type filter  (null = all supported types)
     * @param currency optional currency code filter
     * @param from     optional start date (inclusive)
     * @param to       optional end date   (inclusive)
     * @param agencyId optional agency / user-id filter
     * @param page     0-based page number
     * @param size     page size
     */
    DepositReportDTO getDepositReport(DepositType type,
                                      String currency,
                                      LocalDate from,
                                      LocalDate to,
                                      Long agencyId,
                                      int page,
                                      int size);
}

