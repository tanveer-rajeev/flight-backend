package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.credit.BusinessCreditInfoResponse;
import com.aerionsoft.application.dto.credit.CreditLimitHistoryResponse;
import com.aerionsoft.application.dto.credit.CreditLimitRequest;
import com.aerionsoft.application.dto.credit.CreditRequestDto;
import com.aerionsoft.application.dto.credit.UserCreditRequestInput;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface CreditLimitService {

    /**
     * Grant credit to a business (CREDIT status)
     */
    CreditLimitHistoryResponse grantCredit(CreditLimitRequest request);

    /**
     * Reduce available credit limit when a transaction draws on credit.
     */
    void reduceCreditForUsage(Long userId, BigDecimal creditUsed, String cause, Long performedBy);

    /**
     * Calculate credit drawn by a debit and reduce the business credit limit accordingly.
     */
    void applyCreditUsageForDebit(Long userId, double balanceBefore, double amount, String cause, Long performedBy);


    /**
     * Get credit limit history for a specific business
     */
    Page<CreditLimitHistoryResponse> getCreditHistoryByBusiness(Long businessId, int page, int size);

    /**
     * Get all credit limit history (admin view)
     */
    Page<CreditLimitHistoryResponse> getAllCreditHistory(int page, int size);

    /**
     * Get credit information for a specific business
     */
    BusinessCreditInfoResponse getBusinessCreditInfo(Long businessId);


    /**
     * Get a specific credit history entry by ID
     */
    CreditLimitHistoryResponse getCreditHistoryById(Long id);

    // ==================== Credit Request Methods (User-facing) ====================

    /**
     * User submits a credit request
     */
    CreditRequestDto submitCreditRequest(Long userId, UserCreditRequestInput request);

    /**
     * Get credit requests for a user's business
     */
    Page<CreditRequestDto> getUserCreditRequests(Long userId, int page, int size);

    /**
     * Get all credit requests (admin view)
     */
    Page<CreditRequestDto> getAllCreditRequests(String status, int page, int size);

    /**
     * Admin approves a credit request
     */
    CreditRequestDto approveCreditRequest(Long requestId, BigDecimal approvedAmount, String adminRemarks);

    /**
     * Admin rejects a credit request
     */
    CreditRequestDto rejectCreditRequest(Long requestId, String adminRemarks);

    /**
     * Get credit request by ID
     */
    CreditRequestDto getCreditRequestById(Long requestId);

    /**
     * Get credit info for current user (by userId)
     */
    BusinessCreditInfoResponse getUserCreditInfo(Long userId);
}

