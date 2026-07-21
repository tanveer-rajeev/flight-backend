package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.annotation.SkipAutoAudit;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.credit.BusinessCreditInfoResponse;
import com.aerionsoft.application.dto.credit.CreditLimitHistoryResponse;
import com.aerionsoft.application.dto.credit.CreditLimitRequest;
import com.aerionsoft.application.dto.credit.CreditRequestDto;
import com.aerionsoft.application.service.wallet.CreditLimitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@SkipAutoAudit
@RestController
@RequestMapping("/api/admin/credit-limit")
@RequiredArgsConstructor
@Validated
public class CreditLimitController extends BaseController {

    private final CreditLimitService creditLimitService;

    @PostMapping("/grant")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'manage-credit-limit')")
    public ResponseEntity<BaseResponse<CreditLimitHistoryResponse>> grantCredit(
            @Valid @RequestBody CreditLimitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Credit granted successfully", creditLimitService.grantCredit(request)));
    }

    @GetMapping("/history/business/{businessId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-credit-limit')")
    public ResponseEntity<BaseResponse<Page<CreditLimitHistoryResponse>>> getCreditHistoryByBusiness(
            @PathVariable @Positive Long businessId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(BaseResponse.ok("Credit history retrieved successfully",
                creditLimitService.getCreditHistoryByBusiness(businessId, page, size)));
    }

    @GetMapping("/history")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-credit-limit')")
    public ResponseEntity<BaseResponse<Page<CreditLimitHistoryResponse>>> getAllCreditHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(BaseResponse.ok("Credit history retrieved successfully",
                creditLimitService.getAllCreditHistory(page, size)));
    }

    @GetMapping("/business/{businessId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-credit-limit')")
    public ResponseEntity<BaseResponse<BusinessCreditInfoResponse>> getBusinessCreditInfo(
            @PathVariable @Positive Long businessId) {
        return ResponseEntity.ok(BaseResponse.ok("Business credit info retrieved successfully",
                creditLimitService.getBusinessCreditInfo(businessId)));
    }

    @GetMapping("/history/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-credit-limit')")
    public ResponseEntity<BaseResponse<CreditLimitHistoryResponse>> getCreditHistoryById(
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(BaseResponse.ok("Credit history entry retrieved successfully",
                creditLimitService.getCreditHistoryById(id)));
    }

    @GetMapping("/requests")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-credit-limit')")
    public ResponseEntity<BaseResponse<Page<CreditRequestDto>>> getAllCreditRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(BaseResponse.ok("Credit requests retrieved successfully",
                creditLimitService.getAllCreditRequests(status, page, size)));
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-credit-limit')")
    public ResponseEntity<BaseResponse<CreditRequestDto>> getCreditRequestById(
            @PathVariable @Positive Long requestId) {
        return ResponseEntity.ok(BaseResponse.ok("Credit request retrieved successfully",
                creditLimitService.getCreditRequestById(requestId)));
    }

    @PostMapping("/requests/{requestId}/approve")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'manage-credit-limit')")
    public ResponseEntity<BaseResponse<CreditRequestDto>> approveCreditRequest(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody ApproveRejectRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Credit request approved successfully",
                creditLimitService.approveCreditRequest(requestId, request.getApprovedAmount(), request.getAdminRemarks())));
    }

    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'manage-credit-limit')")
    public ResponseEntity<BaseResponse<CreditRequestDto>> rejectCreditRequest(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody ApproveRejectRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Credit request rejected successfully",
                creditLimitService.rejectCreditRequest(requestId, request.getAdminRemarks())));
    }

    @Data
    public static class ApproveRejectRequest {
        private BigDecimal approvedAmount;
        private String adminRemarks;
    }
}
