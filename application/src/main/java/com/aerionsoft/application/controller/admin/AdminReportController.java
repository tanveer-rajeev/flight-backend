package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.report.AdminSalesReportDTO;
import com.aerionsoft.application.dto.report.AgencyDueReportDTO;
import com.aerionsoft.application.dto.report.CreditListReportDTO;
import com.aerionsoft.application.dto.report.CreditReportDTO;
import com.aerionsoft.application.dto.report.DepositReportDTO;
import com.aerionsoft.application.dto.report.RefundReportDTO;
import com.aerionsoft.application.dto.report.UserBalanceReconciliationReportDTO;
import com.aerionsoft.application.dto.sabre.DailyTicketSegmentListResponse;
import com.aerionsoft.application.dto.sabre.DailyTicketSegmentReloadResponse;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.service.report.AdminSalesReportService;
import com.aerionsoft.application.service.report.AgencyDueReportService;
import com.aerionsoft.application.service.report.CreditListReportService;
import com.aerionsoft.application.service.report.CreditReportService;
import com.aerionsoft.application.service.report.DepositReportService;
import com.aerionsoft.application.service.report.RefundReportService;
import com.aerionsoft.application.service.report.UserBalanceReconciliationService;
import com.aerionsoft.application.service.sabre.DailyTicketSegmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/admin/report")
public class AdminReportController extends BaseController {

    private final AdminSalesReportService adminSalesReportService;
    private final CreditReportService creditReportService;
    private final DepositReportService depositReportService;
    private final AgencyDueReportService agencyDueReportService;
    private final CreditListReportService creditListReportService;
    private final RefundReportService refundReportService;
    private final DailyTicketSegmentService dailyTicketSegmentService;
    private final UserBalanceReconciliationService userBalanceReconciliationService;

    public AdminReportController(AdminSalesReportService adminSalesReportService,
                                 CreditReportService creditReportService,
                                 DepositReportService depositReportService,
                                 AgencyDueReportService agencyDueReportService,
                                 CreditListReportService creditListReportService,
                                 RefundReportService refundReportService,
                                 DailyTicketSegmentService dailyTicketSegmentService,
                                 UserBalanceReconciliationService userBalanceReconciliationService) {
        this.adminSalesReportService = adminSalesReportService;
        this.creditReportService = creditReportService;
        this.depositReportService = depositReportService;
        this.agencyDueReportService = agencyDueReportService;
        this.creditListReportService = creditListReportService;
        this.refundReportService = refundReportService;
        this.dailyTicketSegmentService = dailyTicketSegmentService;
        this.userBalanceReconciliationService = userBalanceReconciliationService;
    }

    @GetMapping("/sales")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-sales-report')")
    public ResponseEntity<BaseResponse<AdminSalesReportDTO>> getSalesReport(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) String airlineCode,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminSalesReportDTO report = adminSalesReportService.getSalesReport(userId, agencyId, airlineCode, currency, from, to, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Sales report retrieved successfully", report));
    }

    @GetMapping("/credit-given")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-credit-report')")
    public ResponseEntity<BaseResponse<CreditReportDTO>> getCreditGivenReport(
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CreditReportDTO report = creditReportService.getCreditGivenReport(from, to, currency, agencyId, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Credit given report retrieved successfully", report));
    }

    @GetMapping("/credit-used")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-credit-report')")
    public ResponseEntity<BaseResponse<CreditReportDTO>> getCreditUsedReport(
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CreditReportDTO report = creditReportService.getCreditUsedReport(from, to, currency, agencyId, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Credit used report retrieved successfully", report));
    }

    /**
     * Deposit list report.
     * Supported types: CASH, CHEQUE, BANK_DEPOSIT, DEPOSIT, BANK_TRANSFER_OR_MFS,
     *                  STRIPE, INSTANT, PURCHASE, NGENIUS, SSL
     * GET /api/admin/report/deposit-list
     */
    @GetMapping("/deposit-list")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-deposit-report')")
    public ResponseEntity<BaseResponse<DepositReportDTO>> getDepositListReport(
            @RequestParam(required = false) DepositType type,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        DepositReportDTO report = depositReportService.getDepositReport(type, currency, from, to, agencyId, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Deposit list report retrieved successfully", report));
    }

    /**
     * Agency Due List — agencies whose balance is negative (they owe money).
     * Each record also carries the agency's configured credit limit.
     *
     * GET /api/admin/report/agency-due
     * Query params:
     *   currency – filter by currency code, e.g. USD (optional, null = all)
     *   sortDir  – "asc" or "desc" (default: asc, most-overdue first)
     *   page     – zero-based page index (default 0)
     *   size     – page size (default 20)
     */
    @GetMapping("/agency-due")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-agency-due-report')")
    public ResponseEntity<BaseResponse<AgencyDueReportDTO>> getAgencyDueReport(
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AgencyDueReportDTO report = agencyDueReportService.getAgencyDueReport(currency, sortDir, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Agency due list retrieved successfully", report));
    }

    /**
     * Agency Credit List — agencies whose balance is positive (they have credit).
     *
     * GET /api/admin/report/agency-credit
     * Query params:
     *   currency – filter by currency code, e.g. USD (optional, null = all)
     *   sortDir  – "asc" or "desc" (default: desc, highest-credit first)
     *   page     – zero-based page index (default 0)
     *   size     – page size (default 20)
     */
    @GetMapping("/agency-credit")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-agency-due-report')")
    public ResponseEntity<BaseResponse<AgencyDueReportDTO>> getAgencyCreditReport(
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AgencyDueReportDTO report = agencyDueReportService.getAgencyCreditReport(currency, sortDir, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Agency credit list retrieved successfully", report));
    }

    /**
     * Credit list — per-agency summary of credit given vs credit used.
     * Only businesses with credit_limit > 0 are included.
     *
     * GET /api/admin/report/credit-list
     * Query params:
     *   agencyId  – filter by a specific agency (optional)
     *   currency  – filter by currency, e.g. USD (optional)
     *   sortDir   – "asc" or "desc" for totalCreditGiven (default: desc)
     *   page      – zero-based page index (default 0)
     *   size      – page size (default 20)
     */
    @GetMapping("/credit-list")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-credit-list-report')")
    public ResponseEntity<BaseResponse<CreditListReportDTO>> getCreditList(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CreditListReportDTO report = creditListReportService.getCreditList(currency, agencyId, sortDir, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Credit list retrieved successfully", report));
    }

    /**
     * Refund report — bookings with REFUND status.
     *
     * GET /api/admin/report/refund
     * Query params:
     *   userId      – filter by specific user/agent (optional)
     *   agencyId    – filter by agency (optional)
     *   airlineCode – filter by airline code (optional)
     *   currency    – filter by currency, e.g. USD (optional)
     *   from        – start date inclusive, ISO format yyyy-MM-dd (optional)
     *   to          – end date inclusive, ISO format yyyy-MM-dd (optional)
     *   page        – zero-based page index (default 0)
     *   size        – page size (default 20)
     */
    @GetMapping("/refund")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-refund-report')")
    public ResponseEntity<BaseResponse<RefundReportDTO>> getRefundReport(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) String airlineCode,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        RefundReportDTO report = refundReportService.getRefundReport(userId, agencyId, airlineCode, currency, from, to, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Refund report retrieved successfully", report));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Daily Ticket Issued Segment Count (Sabre)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Manually reload daily ticket issued segment counts from the Sabre API for a given channel.
     * Only new PNRs not already stored for today's date are saved.
     *
     * GET /api/admin/report/daily-segment/reload?channel=s-bd
     */
    @GetMapping("/daily-segment/reload")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<BaseResponse<DailyTicketSegmentReloadResponse>> reloadDailySegments(
            @RequestParam String channel) {
        DailyTicketSegmentReloadResponse result = dailyTicketSegmentService.fetchAndSave(channel);
        return ResponseEntity.ok(BaseResponse.ok("Daily segment reload complete", result));
    }

    /**
     * List stored daily ticket issued segment records filtered by date (and optionally channel).
     *
     * GET /api/admin/report/daily-segment/list?date=2026-05-13
     * GET /api/admin/report/daily-segment/list?date=2026-05-13&channel=s-bd
     */
    @GetMapping("/daily-segment/list")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<BaseResponse<DailyTicketSegmentListResponse>> listDailySegments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String channel) {
        DailyTicketSegmentListResponse result = (channel != null && !channel.isBlank())
                ? dailyTicketSegmentService.getByDateAndChannel(date, channel)
                : dailyTicketSegmentService.getByDate(date);
        return ResponseEntity.ok(BaseResponse.ok("Daily segments fetched successfully", result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User Balance Reconciliation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Balance reconciliation report — compares each user's wallet balance
     * against the sum of their credit / debit transactions.
     *
     * GET /api/admin/report/balance-reconciliation
     *
     * Query params:
     *   onlyDiscrepancies   – if true, only rows where diff_amount != 0 (default: false)
     *   page                – zero-based page index (default 0)
     *   size                – page size (default 20)
     *   sortBy              – field to sort by, e.g. diffAmount (default: userId)
     *   sortDir             – "asc" or "desc" (default: asc)
     */
    @GetMapping("/balance-reconciliation")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-balance-reconciliation')")
    public ResponseEntity<BaseResponse<UserBalanceReconciliationReportDTO>> getBalanceReconciliation(
            @RequestParam(defaultValue = "false") boolean onlyDiscrepancies,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        UserBalanceReconciliationReportDTO report =
                userBalanceReconciliationService.getReport(onlyDiscrepancies, page, size, sortBy, sortDir);
        return ResponseEntity.ok(BaseResponse.ok("Balance reconciliation report retrieved successfully", report));
    }
}
