package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.service.user.CustomUserDetails;

import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.credit.BusinessCreditInfoResponse;
import com.aerionsoft.application.dto.credit.CreditLimitHistoryResponse;
import com.aerionsoft.application.dto.credit.CreditLimitRequest;
import com.aerionsoft.application.dto.credit.CreditRequestDto;
import com.aerionsoft.application.dto.credit.UserCreditRequestInput;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.CreditLimitHistory;
import com.aerionsoft.application.entity.CreditRequest;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
import com.aerionsoft.application.enums.wallet.CreditRequestStatus;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.wallet.CreditLimitHistoryRepository;
import com.aerionsoft.application.repository.wallet.CreditRequestRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.audit.ActivityAdminAuditSupport;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.util.TimestampMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditLimitServiceImpl implements CreditLimitService {

    private final CreditLimitHistoryRepository creditLimitHistoryRepository;
    private final BusinessRepository businessRepository;
    private final AdminUserRepository adminUserRepository;
    private final CreditRequestRepository creditRequestRepository;
    private final UserRepository userRepository;
    private final TimestampMapper timestampMapper;
    private final BusinessService businessService;
    private final ActivityAdminAuditSupport activityAdminAuditSupport;

    @Override
    @Transactional
    public CreditLimitHistoryResponse grantCredit(CreditLimitRequest request) {
        log.info("Granting credit to business ID: {}, amount: {}", request.getBusinessId(), request.getAmount());

        BusinessEntity business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business", request.getBusinessId()));

        BigDecimal currentCreditLimit = business.getCreditLimit() != null ? business.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal newCreditLimit = currentCreditLimit.add(request.getAmount());

        // Create history record
        CreditLimitHistory history = CreditLimitHistory.builder()
                .businessId(request.getBusinessId())
                .amount(request.getAmount())
                .cause(request.getCause())
                .returnDate(request.getReturnDate())
                .adminInstruction(request.getAdminInstruction())
                .status(CreditLimitStatus.CREDIT)
                .balanceBefore(currentCreditLimit)
                .balanceAfter(newCreditLimit)
                .createdBy(getCurrentAdminId())
                .build();

        creditLimitHistoryRepository.save(history);

        business.setCreditLimit(newCreditLimit);

        businessRepository.save(business);

        log.info("Credit granted successfully. New credit limit for business {}: {}",
                 request.getBusinessId(), newCreditLimit);

        activityAdminAuditSupport.logCreditLimitChange(
                request.getBusinessId(), request.getAmount(), request.getCause());

        return mapToResponse(history, business);
    }

    @Override
    @Transactional
    public void reduceCreditForUsage(Long userId, BigDecimal creditUsed, String cause, Long performedBy) {
        if (creditUsed == null || creditUsed.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        User effectiveUser = user.getParentUser() != null ? user.getParentUser() : user;
        BusinessEntity business = businessRepository.findFirstByMotherUser(effectiveUser).orElse(null);
        if (business == null) {
            return;
        }

        BigDecimal currentCreditLimit = business.getCreditLimit() != null ? business.getCreditLimit() : BigDecimal.ZERO;
        if (currentCreditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal reduction = creditUsed.min(currentCreditLimit);
        BigDecimal newCreditLimit = currentCreditLimit.subtract(reduction);

        business.setCreditLimit(newCreditLimit);
        businessRepository.save(business);

        creditLimitHistoryRepository.save(CreditLimitHistory.builder()
                .businessId(business.getId())
                .amount(reduction)
                .cause(cause != null ? cause : "Credit used for transaction")
                .status(CreditLimitStatus.DEBIT)
                .balanceBefore(currentCreditLimit)
                .balanceAfter(newCreditLimit)
                .createdBy(performedBy)
                .build());

        log.info("Reduced credit limit for business {} from {} to {} (credit used: {})",
                business.getId(), currentCreditLimit, newCreditLimit, reduction);
    }

    @Override
    @Transactional
    public void applyCreditUsageForDebit(Long userId, double balanceBefore, double amount, String cause, Long performedBy) {
        double creditUsed = CreditLimitValidatorService.calculateCreditUsed(balanceBefore, amount);
        if (creditUsed <= 0) {
            return;
        }
        reduceCreditForUsage(userId, BigDecimal.valueOf(creditUsed), cause, performedBy);
    }

    @Override
    public Page<CreditLimitHistoryResponse> getCreditHistoryByBusiness(Long businessId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CreditLimitHistory> historyPage = creditLimitHistoryRepository.findByBusinessId(businessId, pageable);

        return historyPage.map(this::mapToResponseWithLookup);
    }

    @Override
    public Page<CreditLimitHistoryResponse> getAllCreditHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CreditLimitHistory> historyPage = creditLimitHistoryRepository.findAllByOrderByCreatedAtDesc(pageable);

        return historyPage.map(this::mapToResponseWithLookup);
    }

    @Override
    public BusinessCreditInfoResponse getBusinessCreditInfo(Long businessId) {
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        // creditLimit is remaining credit the business can draw on top of existing wallet balance/debt
        BigDecimal creditLimit = business.getCreditLimit() != null ? business.getCreditLimit() : BigDecimal.ZERO;

        // Calculate total credit given and used from history
        List<CreditLimitHistory> history = creditLimitHistoryRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);

        BigDecimal totalCreditGiven = history.stream()
                .filter(h -> h.getStatus() == CreditLimitStatus.CREDIT)
                .map(CreditLimitHistory::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCreditUsed = history.stream()
                .filter(h -> h.getStatus() == CreditLimitStatus.DEBIT)
                .map(CreditLimitHistory::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BusinessCreditInfoResponse.builder()
                .businessId(businessId)
                .businessName(business.getCompanyName())
                .totalCreditGiven(totalCreditGiven)
                .totalCreditUsed(totalCreditUsed)
                .availableCredit(creditLimit)
                .build();
    }



    @Override
    public CreditLimitHistoryResponse getCreditHistoryById(Long id) {
        CreditLimitHistory history = creditLimitHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit limit history", id));

        return mapToResponseWithLookup(history);
    }

    private Long getCurrentAdminId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
                return customUserDetails.getId();
            }
        } catch (Exception e) {
            log.warn("Could not get current admin ID", e);
        }
        return null;
    }

    private CreditLimitHistoryResponse mapToResponse(CreditLimitHistory history, BusinessEntity business) {
        String createdByName = null;
        if (history.getCreatedBy() != null) {
            createdByName = adminUserRepository.findById(history.getCreatedBy())
                    .map(AdminUser::getFullName)
                    .orElse(null);
        }

        return CreditLimitHistoryResponse.builder()
                .id(history.getId())
                .businessId(history.getBusinessId())
                .businessName(business.getCompanyName())
                .amount(history.getAmount())
                .cause(history.getCause())
                .returnDate(history.getReturnDate())
                .createdBy(history.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(timestampMapper.toRequestUserTime(history.getCreatedAt(), history.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(history.getUpdatedAt(), history.getUpdatedTimeOffset() != null ? history.getUpdatedTimeOffset() : history.getCreatedTimeOffset()))
                .adminInstruction(history.getAdminInstruction())
                .status(history.getStatus())
                .balanceBefore(history.getBalanceBefore())
                .balanceAfter(history.getBalanceAfter())
                .build();
    }

    private CreditLimitHistoryResponse mapToResponseWithLookup(CreditLimitHistory history) {
        BusinessEntity business = businessRepository.findById(history.getBusinessId()).orElse(null);
        String businessName = business != null ? business.getCompanyName() : null;

        String createdByName = null;
        if (history.getCreatedBy() != null) {
            createdByName = adminUserRepository.findById(history.getCreatedBy())
                    .map(AdminUser::getFullName)
                    .orElse(null);
        }

        return CreditLimitHistoryResponse.builder()
                .id(history.getId())
                .businessId(history.getBusinessId())
                .businessName(businessName)
                .amount(history.getAmount())
                .cause(history.getCause())
                .returnDate(history.getReturnDate())
                .createdBy(history.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(timestampMapper.toRequestUserTime(history.getCreatedAt(), history.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(history.getUpdatedAt(), history.getUpdatedTimeOffset() != null ? history.getUpdatedTimeOffset() : history.getCreatedTimeOffset()))
                .adminInstruction(history.getAdminInstruction())
                .status(history.getStatus())
                .balanceBefore(history.getBalanceBefore())
                .balanceAfter(history.getBalanceAfter())
                .build();
    }

    // ==================== Credit Request Methods ====================

    @Override
    @Transactional
    public CreditRequestDto submitCreditRequest(Long userId, UserCreditRequestInput request) {
        log.info("User {} submitting credit request for amount: {}", userId, request.getRequestedAmount());

        // Get user's business
        BusinessDto businessDto = businessService.getBusinessByUserId(userId);
        if (businessDto == null || businessDto.getId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "User does not belong to any business");
        }

        // Check if there's already a pending request
        long pendingCount = creditRequestRepository.countByBusinessIdAndStatus(businessDto.getId(), CreditRequestStatus.PENDING);
        if (pendingCount > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "You already have a pending credit request. Please wait for it to be processed.");
        }

        CreditRequest creditRequest = CreditRequest.builder()
                .businessId(businessDto.getId())
                .requestedAmount(request.getRequestedAmount())
                .reason(request.getReason())
                .status(CreditRequestStatus.PENDING)
                .requestedBy(userId)
                .requestedAt(UserDateTimeUtil.now())
                .build();

        creditRequestRepository.save(creditRequest);
        log.info("Credit request submitted successfully. Request ID: {}", creditRequest.getId());

        return mapCreditRequestToDto(creditRequest);
    }

    @Override
    public Page<CreditRequestDto> getUserCreditRequests(Long userId, int page, int size) {
        BusinessDto businessDto = businessService.getBusinessByUserId(userId);
        if (businessDto == null || businessDto.getId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "User does not belong to any business");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CreditRequest> requests = creditRequestRepository.findByBusinessId(businessDto.getId(), pageable);

        return requests.map(this::mapCreditRequestToDto);
    }

    @Override
    public Page<CreditRequestDto> getAllCreditRequests(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (status != null && !status.isEmpty()) {
            CreditRequestStatus requestStatus = CreditRequestStatus.valueOf(status.toUpperCase());
            return creditRequestRepository.findByStatus(requestStatus, pageable).map(this::mapCreditRequestToDto);
        }

        return creditRequestRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapCreditRequestToDto);
    }

    @Override
    @Transactional
    public CreditRequestDto approveCreditRequest(Long requestId, BigDecimal approvedAmount, String adminRemarks) {
        log.info("Approving credit request ID: {}, approved amount: {}", requestId, approvedAmount);

        CreditRequest creditRequest = creditRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit request", requestId));

        if (creditRequest.getStatus() != CreditRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Credit request is not in pending status");
        }

        // Update credit request
        creditRequest.setStatus(CreditRequestStatus.APPROVED);
        creditRequest.setApprovedAmount(approvedAmount);
        creditRequest.setAdminRemarks(adminRemarks);
        creditRequest.setProcessedBy(getCurrentAdminId());
        creditRequest.setProcessedAt(UserDateTimeUtil.now());
        creditRequestRepository.save(creditRequest);

        // Grant the credit
        CreditLimitRequest grantRequest = CreditLimitRequest.builder()
                .businessId(creditRequest.getBusinessId())
                .amount(approvedAmount)
                .cause("Approved credit request #" + requestId + ": " + creditRequest.getReason())
                .adminInstruction(adminRemarks)
                .build();

        grantCredit(grantRequest);

        activityAdminAuditSupport.logCreditRequestDecision(
                requestId,
                creditRequest.getBusinessId(),
                ActivityEventType.CREDIT_REQUEST_APPROVED,
                adminRemarks);

        log.info("Credit request {} approved and credit granted", requestId);

        return mapCreditRequestToDto(creditRequest);
    }

    @Override
    @Transactional
    public CreditRequestDto rejectCreditRequest(Long requestId, String adminRemarks) {
        log.info("Rejecting credit request ID: {}", requestId);

        CreditRequest creditRequest = creditRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit request", requestId));

        if (creditRequest.getStatus() != CreditRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Credit request is not in pending status");
        }

        creditRequest.setStatus(CreditRequestStatus.REJECTED);
        creditRequest.setAdminRemarks(adminRemarks);
        creditRequest.setProcessedBy(getCurrentAdminId());
        creditRequest.setProcessedAt(UserDateTimeUtil.now());
        creditRequestRepository.save(creditRequest);

        activityAdminAuditSupport.logCreditRequestDecision(
                requestId,
                creditRequest.getBusinessId(),
                ActivityEventType.CREDIT_REQUEST_REJECTED,
                adminRemarks);

        log.info("Credit request {} rejected", requestId);

        return mapCreditRequestToDto(creditRequest);
    }

    @Override
    public CreditRequestDto getCreditRequestById(Long requestId) {
        CreditRequest creditRequest = creditRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit request", requestId));

        return mapCreditRequestToDto(creditRequest);
    }

    @Override
    public BusinessCreditInfoResponse getUserCreditInfo(Long userId) {
        BusinessDto businessDto = businessService.getBusinessByUserId(userId);
        if (businessDto == null || businessDto.getId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "User does not belong to any business");
        }

        return getBusinessCreditInfo(businessDto.getId());
    }

    private CreditRequestDto mapCreditRequestToDto(CreditRequest request) {
        String businessName = businessRepository.findById(request.getBusinessId())
                .map(BusinessEntity::getCompanyName)
                .orElse(null);

        String requestedByName = null;
        if (request.getRequestedBy() != null) {
            requestedByName = userRepository.findById(request.getRequestedBy())
                    .map(User::getFullName)
                    .orElse(null);
        }

        String processedByName = null;
        if (request.getProcessedBy() != null) {
            processedByName = adminUserRepository.findById(request.getProcessedBy())
                    .map(AdminUser::getFullName)
                    .orElse(null);
        }

        return CreditRequestDto.builder()
                .id(request.getId())
                .businessId(request.getBusinessId())
                .businessName(businessName)
                .requestedAmount(request.getRequestedAmount())
                .reason(request.getReason())
                .requestStatus(request.getStatus().name())
                .requestedBy(request.getRequestedBy())
                .requestedByName(requestedByName)
                .requestedAt(request.getRequestedAt())
                .processedBy(request.getProcessedBy())
                .processedByName(processedByName)
                .processedAt(request.getProcessedAt())
                .adminRemarks(request.getAdminRemarks())
                .approvedAmount(request.getApprovedAmount())
                .build();
    }
}

