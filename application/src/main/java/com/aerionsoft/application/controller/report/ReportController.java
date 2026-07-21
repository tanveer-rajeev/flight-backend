package com.aerionsoft.application.controller.report;

import org.springframework.validation.annotation.Validated;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.report.InvoiceReportWithTotalDTO;
import com.aerionsoft.application.service.report.ReportService;

@RestController
@Validated
@RequestMapping("/api/report")
public class ReportController extends BaseController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/invoice-expense")
    public ResponseEntity<BaseResponse<InvoiceReportWithTotalDTO>> getInvoiceExpenseDetail(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        InvoiceReportWithTotalDTO result = reportService.getInvoiceExpenseDetail(provider, authUserId, page, size, from, to);

        return ResponseEntity.ok(BaseResponse.ok(result));
    }
}
