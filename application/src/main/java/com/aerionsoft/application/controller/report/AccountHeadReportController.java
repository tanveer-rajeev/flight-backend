package com.aerionsoft.application.controller.report;

import org.springframework.validation.annotation.Validated;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.report.AccountHeadReportResponseWithTotalDTO;
import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.service.report.AccountHeadReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/account-head-report")
public class AccountHeadReportController extends BaseController {

    private final AccountHeadReportService accountHeadReportService;

    public AccountHeadReportController(AccountHeadReportService accountHeadReportService) {
        this.accountHeadReportService = accountHeadReportService;
    }

    @GetMapping
    public ResponseEntity<BaseResponse<AccountHeadReportResponseWithTotalDTO>> getAccountHeadReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AccountHeadType type,
            @RequestParam(required = false) Long accountHeadId,
            Authentication authentication)
    {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        AccountHeadReportResponseWithTotalDTO response = accountHeadReportService.getAccountHeadReport(provider, authUserId, from, to, type, accountHeadId, page, size);

        return ResponseEntity.ok(BaseResponse.ok(response));
    }
}
