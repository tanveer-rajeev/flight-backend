package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.credit.BusinessCreditInfoResponse;
import com.aerionsoft.application.dto.credit.CreditLimitHistoryResponse;
import com.aerionsoft.application.dto.credit.CreditRequestDto;
import com.aerionsoft.application.dto.credit.UserCreditRequestInput;
import com.aerionsoft.application.service.wallet.CreditLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/user/credit")
@RequiredArgsConstructor
public class UserCreditController extends BaseController {

    private final CreditLimitService creditLimitService;

    /**
     * Get current user's credit information
     */
    @GetMapping("/info")
    public ResponseEntity<BaseResponse<BusinessCreditInfoResponse>> getCreditInfo() {
        Long userId = getUserIdFromAuthentication();
        BusinessCreditInfoResponse creditInfo = creditLimitService.getUserCreditInfo(userId);
        return ResponseEntity.ok(BaseResponse.ok(creditInfo));
    }

    /**
     * Get current user's credit history
     */
    @GetMapping("/history")
    public ResponseEntity<BaseResponse<Page<CreditLimitHistoryResponse>>> getCreditHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserIdFromAuthentication();
        BusinessCreditInfoResponse creditInfo = creditLimitService.getUserCreditInfo(userId);
        Page<CreditLimitHistoryResponse> history = creditLimitService.getCreditHistoryByBusiness(
                creditInfo.getBusinessId(), page, size);
        return ResponseEntity.ok(BaseResponse.ok(history));
    }

    /**
     * Submit a credit request
     */
    @PostMapping("/request")
    public ResponseEntity<BaseResponse<CreditRequestDto>> submitCreditRequest(
            @Valid @RequestBody UserCreditRequestInput request) {
        Long userId = getUserIdFromAuthentication();
        CreditRequestDto creditRequest = creditLimitService.submitCreditRequest(userId, request);
        return ResponseEntity.ok(BaseResponse.ok(creditRequest));
    }

    /**
     * Get current user's credit requests
     */
    @GetMapping("/requests")
    public ResponseEntity<BaseResponse<Page<CreditRequestDto>>> getUserCreditRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserIdFromAuthentication();
        Page<CreditRequestDto> requests = creditLimitService.getUserCreditRequests(userId, page, size);
        return ResponseEntity.ok(BaseResponse.ok(requests));
    }

    /**
     * Get a specific credit request by ID
     */
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<BaseResponse<CreditRequestDto>> getCreditRequestById(@PathVariable Long requestId) {
        CreditRequestDto request = creditLimitService.getCreditRequestById(requestId);
        return ResponseEntity.ok(BaseResponse.ok(request));
    }
}

