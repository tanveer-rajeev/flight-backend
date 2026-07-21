package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.CreditReportDTO;

import java.time.LocalDate;

public interface CreditReportService {

    CreditReportDTO getCreditGivenReport(LocalDate from, LocalDate to, String currency, Long agencyId, int page, int size);

    CreditReportDTO getCreditUsedReport(LocalDate from, LocalDate to, String currency, Long agencyId, int page, int size);
}