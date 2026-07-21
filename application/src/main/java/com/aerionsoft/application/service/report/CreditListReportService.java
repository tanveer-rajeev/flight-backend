package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.CreditListReportDTO;

public interface CreditListReportService {

    /**
     * Returns a paginated list of businesses with credit_limit > 0.
     *
     * @param currency filter by currency, e.g. "AED" (optional)
     * @param agencyId filter by a specific business id (optional, null = all)
     * @param sortDir  sort direction for creditLimit: "asc" or "desc" (default: desc)
     * @param page     zero-based page index
     * @param size     page size
     */
    CreditListReportDTO getCreditList(String currency, Long agencyId, String sortDir, int page, int size);
}
