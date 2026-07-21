package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.wallet.ServiceBalanceDeductionRequest;
import com.aerionsoft.application.dto.wallet.ServiceBalanceDeductionResponse;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.tour.TourApplication;
import com.aerionsoft.application.entity.visa.VisaApplication;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.enums.wallet.ServiceDeductionType;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.repository.tour.TourApplicationRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.visa.VisaApplicationRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.audit.ActivityAdminAuditSupport;
import com.aerionsoft.application.service.user.UserService;
import com.aerionsoft.application.util.Helper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ServiceBalanceDeductionService {

    private static final Logger log = LoggerFactory.getLogger(ServiceBalanceDeductionService.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final CreditLimitValidatorService creditLimitValidatorService;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final WalletDepositRepository walletDepositRepository;
    private final TransactionRepository transactionRepository;
    private final VisaApplicationRepository visaApplicationRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final ActivityAdminAuditSupport activityAdminAuditSupport;

    public ServiceBalanceDeductionService(UserRepository userRepository,
                                          UserService userService,
                                          CreditLimitValidatorService creditLimitValidatorService,
                                          ReferenceGeneratorService referenceGeneratorService,
                                          WalletDepositRepository walletDepositRepository,
                                          TransactionRepository transactionRepository,
                                          VisaApplicationRepository visaApplicationRepository,
                                          TourApplicationRepository tourApplicationRepository,
                                          ActivityAdminAuditSupport activityAdminAuditSupport) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.creditLimitValidatorService = creditLimitValidatorService;
        this.referenceGeneratorService = referenceGeneratorService;
        this.walletDepositRepository = walletDepositRepository;
        this.transactionRepository = transactionRepository;
        this.visaApplicationRepository = visaApplicationRepository;
        this.tourApplicationRepository = tourApplicationRepository;
        this.activityAdminAuditSupport = activityAdminAuditSupport;
    }

    @Transactional(rollbackFor = Exception.class)
    public ServiceBalanceDeductionResponse deduct(ServiceBalanceDeductionRequest request,
                                                  Long targetUserId,
                                                  Long actingUserId,
                                                  String providerName) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        User walletUser = CreditLimitValidatorService.resolveWalletUser(user);
        double currentBalance = walletUser.getBalance() != null ? walletUser.getBalance() : 0.0;
        double availableBalance = creditLimitValidatorService.getAvailableBalance(targetUserId, currentBalance);

        ServiceDeductionType serviceType = request.getServiceType();
        TransactionSourceType sourceType = serviceType.toTransactionSourceType();

        validateSource(serviceType, request.getSourceId(), targetUserId, actingUserId);

        if (!creditLimitValidatorService.hasSufficientBalance(targetUserId, currentBalance, request.getAmount())) {
            throw ServiceExceptions.insufficientBalance(
                    "Insufficient balance to deduct for " + serviceType.name().toLowerCase()
                            + ". Required: " + Helper.formatMoney(request.getAmount())
                            + ", Available: " + Helper.formatMoney(availableBalance));
        }

        String deductionProvider = providerName != null ? providerName : serviceType.name();
        userService.deductUserBalance(
                targetUserId,
                request.getAmount(),
                deductionProvider,
                false,
                "ServiceBalanceDeductionService",
                request.getSourceId(),
                sourceType.name(),
                actingUserId != null ? actingUserId : targetUserId);

        double balanceAfter = currentBalance - request.getAmount();
        String reference = referenceGeneratorService.nextReference(referencePrefix(serviceType));
        String description = buildDescription(request, serviceType);

        WalletDeposit deposit = WalletDeposit.builder()
                .userId(targetUserId)
                .actingUserId(actingUserId)
                .type(DepositType.PURCHASE)
                .status(DepositStatus.APPROVED)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .exchangeRate(1.0)
                .exchangedAmount(request.getAmount())
                .remarks(description)
                .reference(reference)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(UserDateTimeUtil.now())
                .approvedAt(UserDateTimeUtil.now())
                .build();

        deposit = walletDepositRepository.save(deposit);

        Transaction transaction = Transaction.builder()
                .type(DepositType.PURCHASE.name())
                .amount(request.getAmount())
                .currency(request.getCurrency().name())
                .exchangeRate(1.0)
                .convertedAmount(String.valueOf(request.getAmount()))
                .description(description)
                .userId(targetUserId)
                .createdBy(actingUserId != null ? String.valueOf(actingUserId) : String.valueOf(targetUserId))
                .createdAt(UserDateTimeUtil.now())
                .sourceType(sourceType.name())
                .sourceId(request.getSourceId())
                .reference(reference)
                .active(true)
                .build();

        transaction = transactionRepository.saveAndFlush(transaction);

        log.info("Deducted {} {} from user {} for {} (sourceId={}, ref={})",
                request.getAmount(), request.getCurrency(), targetUserId, serviceType, request.getSourceId(), reference);

        activityAdminAuditSupport.logBalanceMovement(
                ActivityEventType.BALANCE_DEBIT,
                targetUserId,
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency().name() : null,
                description,
                reference,
                sourceType.name(),
                request.getSourceId());

        return ServiceBalanceDeductionResponse.builder()
                .transactionId(transaction.getId())
                .depositId(deposit.getId())
                .reference(reference)
                .serviceType(serviceType)
                .sourceId(request.getSourceId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .balanceBefore(currentBalance)
                .balanceAfter(balanceAfter)
                .availableBalanceBefore(availableBalance)
                .build();
    }

    private void validateSource(ServiceDeductionType serviceType,
                                Long sourceId,
                                Long targetUserId,
                                Long actingUserId) {
        if (sourceId == null) {
            return;
        }

        Long ownerUserId = actingUserId != null ? actingUserId : targetUserId;
        String ownerUserIdStr = ownerUserId.toString();

        switch (serviceType) {
            case VISA -> {
                VisaApplication application = visaApplicationRepository.findById(sourceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Visa application", sourceId));
                if (application.getCreatedBy() != null
                        && !application.getCreatedBy().equals(ownerUserIdStr)
                        && !application.getCreatedBy().equals(targetUserId.toString())) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "Visa application does not belong to the requesting user");
                }
            }
            case TOUR -> {
                TourApplication application = tourApplicationRepository.findById(sourceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tour application", sourceId));
                if (application.getCreatedBy() != null
                        && !application.getCreatedBy().equals(ownerUserIdStr)
                        && !application.getCreatedBy().equals(targetUserId.toString())) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "Tour application does not belong to the requesting user");
                }
            }
            case HOTEL -> {
                // Hotel bookings are not persisted in this service yet; sourceId is stored as a reference only.
            }
        }
    }

    private String buildDescription(ServiceBalanceDeductionRequest request, ServiceDeductionType serviceType) {
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            return request.getDescription().trim();
        }
        if (request.getSourceId() != null) {
            return serviceType.name().toLowerCase() + " deduction for reference " + request.getSourceId();
        }
        return serviceType.name().toLowerCase() + " service deduction";
    }

    private String referencePrefix(ServiceDeductionType serviceType) {
        return switch (serviceType) {
            case VISA -> "vs";
            case TOUR -> "tr";
            case HOTEL -> "ht";
        };
    }
}
