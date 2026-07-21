package com.aerionsoft.application.controller.report;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.report.SupplierTransactionCreateDTO;
import com.aerionsoft.application.dto.client.invoice.response.SupplierTransactionHistoryDTO;
import com.aerionsoft.application.dto.client.invoice.response.SupplierTransactionHistoryWithTotalDTO;
import com.aerionsoft.application.service.report.SupplierTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/supplier-transaction-histories")
public class SupplierTransactionReportController extends BaseController {
    @Autowired
    SupplierTransactionService supplierTransactionHistoryService;

    @GetMapping
    public ResponseEntity<BaseResponse<SupplierTransactionHistoryWithTotalDTO>> getTransactionHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long ledgerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        SupplierTransactionHistoryWithTotalDTO result = supplierTransactionHistoryService.getTransactionHistories(provider, authUserId, page, size, supplierId, ledgerId, from, to);

        return ResponseEntity.ok(BaseResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<SupplierTransactionHistoryDTO>> createTransactionHistory(@Valid @RequestBody SupplierTransactionCreateDTO request, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        SupplierTransactionHistoryDTO result = supplierTransactionHistoryService.createSupplierTransactionHistory(provider, authUserId, request);

        return ResponseEntity.ok(BaseResponse.ok(result));
    }
}
