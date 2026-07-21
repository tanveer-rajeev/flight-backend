package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.AgencyDueReportDTO;

public interface AgencyDueReportService {

    /**
     * Returns a paginated list of agencies that have a negative balance (they owe money).
     *
     * @param currency filter by currency code (e.g. "USD"), null means all currencies
     * @param sortDir  "asc" or "desc" – sort direction by balance (default: asc)
     * @param page     zero-based page index
     * @param size     page size
     */
    AgencyDueReportDTO getAgencyDueReport(String currency, String sortDir, int page, int size);

    /**
     * Returns a paginated list of agencies that have a positive balance (they have credit).
     *
     * @param currency filter by currency code (e.g. "USD"), null means all currencies
     * @param sortDir  "asc" or "desc" – sort direction by balance (default: desc)
     * @param page     zero-based page index
     * @param size     page size
     */
    AgencyDueReportDTO getAgencyCreditReport(String currency, String sortDir, int page, int size);
}
