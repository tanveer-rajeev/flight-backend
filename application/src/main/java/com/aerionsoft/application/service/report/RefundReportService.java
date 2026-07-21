package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.RefundReportDTO;

import java.time.LocalDate;

public interface RefundReportService {

    /**
     * Returns a paginated refund report for bookings with REFUND status.
     *
     * @param userId      optional user/agent filter
     * @param agencyId    optional agency filter
     * @param airlineCode optional airline code filter
     * @param currency    optional currency filter
     * @param from        optional start date (inclusive)
     * @param to          optional end date (inclusive)
     * @param page        0-based page number
     * @param size        page size
     */
    RefundReportDTO getRefundReport(Long userId,
                                    Long agencyId,
                                    String airlineCode,
                                    String currency,
                                    LocalDate from,
                                    LocalDate to,
                                    int page,
                                    int size);
}

