package com.aerionsoft.application.controller.wallet;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.ledger.LedgerPageResponse;
import com.aerionsoft.application.dto.ledger.LedgerPaginatedResponse;
import com.aerionsoft.application.dto.ledger.LedgerResponse;
import com.aerionsoft.application.enums.wallet.TransactionStatus;
import com.aerionsoft.application.service.wallet.LedgerService;
import com.aerionsoft.application.service.user.UserService;
import com.aerionsoft.application.service.wallet.WalletService;
import com.aerionsoft.application.service.admin.AdminUserService;
import com.aerionsoft.application.util.UserDateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/ledger")
public class LedgerController extends BaseController {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private WalletService walletService;

    /**
     * Get ledger (transaction history with deposit/booking details) for the authenticated user
     */
    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-ledger')")
    public ResponseEntity<BaseResponse<LedgerPaginatedResponse>> getLedger(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus transactionStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long userId = getUserId(authentication);
        boolean admin = isAdmin();
        LocalDate effectiveFrom = from != null ? from : UserDateTimeUtil.now().toLocalDate();
        LocalDate effectiveTo = to != null ? to : effectiveFrom;

        LedgerPaginatedResponse ledger;
        if (admin) {
            ledger = ledgerService.getAllLedgerPagedWithOpeningBalance(page, size, transactionStatus, effectiveFrom, effectiveTo);
        } else {
            ledger = ledgerService.getLedgerPagedWithOpeningBalance(userId, page, size, transactionStatus, effectiveFrom, effectiveTo);
        }

        return ResponseEntity.ok(BaseResponse.ok("Ledger retrieved successfully", ledger));
    }

    /**
     * Get ledger for a specific user (admin only)
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-all-ledger')")
    public ResponseEntity<BaseResponse<LedgerPageResponse>> getUserLedger(
            @PathVariable Long userId,
            @RequestParam(required = false) TransactionStatus transactionStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LedgerPageResponse ledger = ledgerService.getLedgerWithOpeningBalance(userId, transactionStatus, from, to);
        return ResponseEntity.ok(BaseResponse.ok("Ledger retrieved successfully", ledger));
    }

    /**
     * Get ledger statement within a date range for the authenticated user
     */
    @GetMapping("/statement")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-ledger')")
    public ResponseEntity<BaseResponse<List<LedgerResponse>>> getLedgerStatement(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long userId = getUserId(authentication);
        List<LedgerResponse> statement = ledgerService.getLedgerByDateRange(userId, from, to);
        return ResponseEntity.ok(BaseResponse.ok("Ledger statement retrieved successfully", statement));
    }

    /**
     * Get ledger statement for a specific user within a date range (admin only)
     */
    @GetMapping("/statement/user/{userId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-all-ledger')")
    public ResponseEntity<BaseResponse<List<LedgerResponse>>> getUserLedgerStatement(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<LedgerResponse> statement = ledgerService.getLedgerByDateRange(userId, from, to);
        return ResponseEntity.ok(BaseResponse.ok("Ledger statement retrieved successfully", statement));
    }

    /**
     * Hard-delete a transaction by transactionId and reverse its wallet balance effect.
     * Linked wallet deposit is removed first, then the transaction row.
     * CREDIT deletions deduct the amount; DEBIT deletions refund the amount back to the wallet.
     */
    @DeleteMapping("/transaction/{transactionId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-transaction')")
    public ResponseEntity<BaseResponse<Void>> deleteTransaction(
            @PathVariable Long transactionId
    ) {
        walletService.deleteTransaction(transactionId);
        return ResponseEntity.ok(BaseResponse.<Void>ok("Transaction deleted and balance adjusted successfully"));
    }

    private Long getUserId(Authentication authentication) {
        if (isAdmin()) {
            return adminUserService.getUserByEmail(authentication.getName()).getId();
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}

