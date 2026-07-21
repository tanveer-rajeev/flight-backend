package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.bank.*;
import com.aerionsoft.application.service.wallet.BankLedgerService;
import com.aerionsoft.application.service.wallet.DepositBankService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/deposit-banks")
@Validated
@RequiredArgsConstructor
public class DepositBankController extends BaseController {

    private final DepositBankService depositBankService;
    private final BankLedgerService bankLedgerService;

    @GetMapping("/list")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-bank-deposit')")
    public ResponseEntity<BaseResponse<List<DepositBankResponse>>> list() {
        List<DepositBankResponse> banks = depositBankService.getAll();
        return ResponseEntity.ok(BaseResponse.ok("Deposit banks retrieved successfully", banks));
    }

    @PostMapping("/create")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-bank-deposit')")
    public ResponseEntity<BaseResponse<DepositBankResponse>> create(@Valid @RequestBody DepositBankRequest request) {
        DepositBankResponse saved = depositBankService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Deposit bank created successfully", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-bank-deposit')")
    public ResponseEntity<BaseResponse<DepositBankResponse>> update(
            @PathVariable @Positive(message = "Deposit bank id must be a positive number") Long id,
            @Valid @RequestBody DepositBankRequest request) {
        DepositBankResponse updated = depositBankService.update(id, request);
        return ResponseEntity.ok(BaseResponse.ok("Deposit bank updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-bank-deposit')")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable @Positive(message = "Deposit bank id must be a positive number") Long id) {
        depositBankService.delete(id);
        return ResponseEntity.ok(BaseResponse.ok("Deposit bank deleted successfully"));
    }

    @GetMapping("/{bankId}/ledger")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-bank-ledger')")
    public ResponseEntity<BaseResponse<Page<BankLedgerEntryResponse>>> ledger(
            @PathVariable @Positive Long bankId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<BankLedgerEntryResponse> result = bankLedgerService.getLedger(bankId, from, to, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Bank ledger retrieved successfully", result));
    }

    @GetMapping("/{bankId}/statement")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-bank-ledger')")
    public ResponseEntity<BaseResponse<BankStatementResponse>> statement(
            @PathVariable @Positive Long bankId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        BankStatementResponse result = bankLedgerService.getStatement(bankId, from, to);
        return ResponseEntity.ok(BaseResponse.ok("Bank statement retrieved successfully", result));
    }

    @GetMapping("/summary/today")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-bank-ledger')")
    public ResponseEntity<BaseResponse<List<BankTodaySummaryItemResponse>>> todaySummary() {
        List<BankTodaySummaryItemResponse> result = bankLedgerService.getTodaySummary();
        return ResponseEntity.ok(BaseResponse.ok("Bank today summary retrieved successfully", result));
    }

    @PostMapping("/{bankId}/deposit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-bank-cash-deposit')")
    public ResponseEntity<BaseResponse<BankLedgerEntryResponse>> deposit(
            @PathVariable @Positive Long bankId,
            @Valid @RequestBody BankDepositRequest request
    ) {
        BankLedgerEntryResponse entry = bankLedgerService.recordDeposit(bankId, request, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Bank deposit recorded successfully", entry));
    }

    @PostMapping("/{bankId}/withdraw")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-bank-withdrawal')")
    public ResponseEntity<BaseResponse<BankLedgerEntryResponse>> withdraw(
            @PathVariable @Positive Long bankId,
            @Valid @RequestBody BankWithdrawRequest request
    ) {
        BankLedgerEntryResponse entry = bankLedgerService.recordWithdrawal(bankId, request, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Bank withdrawal recorded successfully", entry));
    }
}
