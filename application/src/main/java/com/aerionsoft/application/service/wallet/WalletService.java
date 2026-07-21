package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.util.DepositBankMapper;
import com.aerionsoft.application.util.DepositTypeUtil;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.service.common.CurrencyService;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.enums.wallet.TransactionStatus;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import com.aerionsoft.application.enums.common.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.aerionsoft.application.repository.wallet.WalletDepositListDao;
import com.aerionsoft.application.repository.wallet.DepositListFilter;
import com.aerionsoft.application.repository.wallet.WalletDepositSpec;
import com.aerionsoft.application.util.FilterRangeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerionsoft.application.dto.admin.bank.AdminChargeRequest;
import com.aerionsoft.application.dto.admin.bank.AdminDepositRequest;
import com.aerionsoft.application.dto.admin.bank.CheckoutSessionResponse;
import com.aerionsoft.application.dto.admin.bank.DepositCurrencySummaryDto;
import com.aerionsoft.application.dto.admin.bank.DepositRequest;
import com.aerionsoft.application.dto.admin.bank.TodayDepositsSummaryResponse;
import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import com.aerionsoft.application.dto.admin.bank.WalletStatementResponse;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.wallet.CreditLimitHistoryRepository;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.paymentGateway.StripeCredentials;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.entity.wallet.BalanceChangeHistory;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.wallet.BalanceChangeHistoryRepository;
import com.aerionsoft.application.repository.wallet.DepositBankRepository;
import com.aerionsoft.application.repository.payment.StripeCredRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.service.notification.NotificationHelper;
import com.aerionsoft.application.service.audit.ActivityAdminAuditSupport;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class WalletService {

    @Autowired
    private WalletDepositRepository depositRepo;
    @Autowired
    private WalletDepositListDao walletDepositListDao;
    @Autowired
    private DepositBankRepository bankRepo;

    @Autowired
    private BankLedgerService bankLedgerService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private StripeCredRepository stripeCredRepository;

    @Autowired
    private TransactionRepository transactionRepo;

    @Value("${app.domain}")
    private String domain;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ReferenceGeneratorService referenceGeneratorService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private NotificationHelper notificationHelper;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private CreditLimitValidatorService creditLimitValidatorService;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @Autowired
    private BalanceChangeHistoryRepository balanceChangeHistoryRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private CreditLimitHistoryRepository creditLimitHistoryRepository;

    @Autowired
    private CreditLimitService creditLimitService;

    @Autowired
    private ActivityAdminAuditSupport activityAdminAuditSupport;

    @Autowired
    private TimestampMapper timestampMapper;

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    /**
     * Get deposits for a user, including those where they acted on behalf of
     * their parent If user has a parent, also fetch parent's deposits
     */
    public Page<WalletDepositResponse> getUserDeposits(String currencyCode, Long userId, int page, int size, boolean admin, DepositStatus statusFilter, DepositType typeFilter, String provider, Boolean isDebit) {
        return getUserDeposits(currencyCode, userId, page, size, admin, statusFilter, typeFilter, provider, isDebit, null, null, false);
    }

    public Page<WalletDepositResponse> getUserDeposits(String currencyCode, Long userId, int page, int size, boolean admin,
                                                        DepositStatus statusFilter, DepositType typeFilter, String provider,
                                                        Boolean isDebit, LocalDate from, LocalDate to) {
        return getUserDeposits(currencyCode, userId, page, size, admin, statusFilter, typeFilter, provider, isDebit, from, to, false);
    }

    @Transactional(readOnly = true)
    public Page<WalletDepositResponse> getUserDeposits(String currencyCode, Long userId, int page, int size, boolean admin,
                                                        DepositStatus statusFilter, DepositType typeFilter, String provider,
                                                        Boolean isDebit, LocalDate from, LocalDate to, boolean includeTotal) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Currency currency = resolveDepositCurrency(currencyCode, userId, admin);
        DepositListFilter filter = buildDepositListFilter(
                admin, userId, statusFilter, typeFilter, isDebit, currency, from, to);

        Page<WalletDeposit> depositPage = walletDepositListDao.findDeposits(filter, pageRequest, includeTotal);
        return mapDepositPage(depositPage, true);
    }

    private DepositListFilter buildDepositListFilter(
            boolean admin,
            Long userId,
            DepositStatus statusFilter,
            DepositType typeFilter,
            Boolean isDebit,
            Currency currency,
            LocalDate from,
            LocalDate to
    ) {
        Long scopedUserId = null;
        Long actingUserId = null;

        if (!admin) {
            Optional<User> userOpt = userRepo.findById(userId);
            if (userOpt.isPresent() && userOpt.get().getParentUser() != null) {
                scopedUserId = userOpt.get().getParentUser().getId();
                actingUserId = userId;
            } else {
                scopedUserId = userId;
            }
        }

        List<DepositStatus> statuses = null;
        List<DepositType> types = null;

        if (admin) {
            if (statusFilter != null) {
                statuses = List.of(statusFilter);
            } else if (Boolean.FALSE.equals(isDebit) || typeFilter == null) {
                statuses = List.of(DepositStatus.APPROVED, DepositStatus.REJECTED);
            }

            if (typeFilter != null) {
                types = List.of(typeFilter);
            } else if (Boolean.FALSE.equals(isDebit)) {
                types = DepositType.getDepositTypes();
            }
        } else {
            if (statusFilter != null) {
                statuses = List.of(statusFilter);
            }
            if (typeFilter != null) {
                types = List.of(typeFilter);
            }
        }

        FilterRangeUtil.InstantRange range = FilterRangeUtil.userDateRange(from, to);
        return new DepositListFilter(
                admin,
                scopedUserId,
                actingUserId,
                statuses,
                types,
                currency,
                range.start(),
                range.endExclusive()
        );
    }

    private Currency resolveDepositCurrency(String currencyCode, Long userId, boolean admin) {
        if (currencyCode != null && "all".equalsIgnoreCase(currencyCode.trim())) {
            return null;
        }

        Set<String> allowedCurrencies = Arrays.stream(Currency.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        if (currencyCode == null || currencyCode.isEmpty()
                || !allowedCurrencies.contains(currencyCode.toUpperCase())) {
            if (admin) {
                AdminUser adminUser = adminUserRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User"));
                currencyCode = adminUser.getCurrency();
            } else {
                User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));
                User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
                currencyCode = agencyUser.getCurrency() == null ? "USD" : agencyUser.getCurrency();
            }
        }

        if (currencyCode == null || currencyCode.isEmpty()) {
            return null;
        }
        return Currency.getIndexFromCode(currencyCode);
    }

    private Page<WalletDepositResponse> mapDepositPage(Page<WalletDeposit> page, boolean includeApprovedBy) {
        DepositMappingContext context = buildMappingContext(page.getContent());
        return page.map(deposit -> includeApprovedBy
                ? mapToResponse(deposit, context)
                : mapToResponse1(deposit, context));
    }

    private record DepositMappingContext(
            Map<Long, User> usersById,
            Map<Long, AdminUser> adminUsersById,
            Map<Long, String> agencyNameByUserId
    ) {}

    private DepositMappingContext buildMappingContext(List<WalletDeposit> deposits) {
        if (deposits == null || deposits.isEmpty()) {
            return new DepositMappingContext(Map.of(), Map.of(), Map.of());
        }

        Set<Long> userIds = deposits.stream()
                .map(WalletDeposit::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> approverIds = deposits.stream()
                .map(WalletDeposit::getApprovedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> usersById = userIds.isEmpty()
                ? Map.of()
                : userRepo.findByIdInWithParent(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));

        Map<Long, AdminUser> adminUsersById = approverIds.isEmpty()
                ? Map.of()
                : adminUserRepository.findAllById(approverIds).stream()
                .collect(Collectors.toMap(AdminUser::getId, admin -> admin, (left, right) -> left));

        Set<Long> motherUserIds = new HashSet<>();
        for (Long userId : userIds) {
            User user = usersById.get(userId);
            if (user == null) {
                motherUserIds.add(userId);
                continue;
            }
            if (user.getParentUser() != null) {
                motherUserIds.add(user.getParentUser().getId());
            } else {
                motherUserIds.add(user.getId());
            }
        }

        Map<Long, String> agencyNameByMotherUserId = motherUserIds.isEmpty()
                ? Map.of()
                : businessRepository.findCompanyNamesByMotherUserIds(motherUserIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> (String) row[1],
                        (left, right) -> left
                ));

        Map<Long, String> agencyNameByUserId = new HashMap<>();
        for (Long userId : userIds) {
            User user = usersById.get(userId);
            Long motherUserId = user != null && user.getParentUser() != null
                    ? user.getParentUser().getId()
                    : userId;
            String agencyName = agencyNameByMotherUserId.get(motherUserId);
            if (agencyName != null) {
                agencyNameByUserId.put(userId, agencyName);
            }
        }

        return new DepositMappingContext(usersById, adminUsersById, agencyNameByUserId);
    }

    @Transactional
    public WalletDepositResponse createDeposit(Long userId, DepositRequest req, String attachmentFilename) throws Exception {
        return createDeposit(userId, req, attachmentFilename, null);
    }

    @Transactional
    public WalletDepositResponse createDeposit(Long userId, DepositRequest req, String attachmentFilename, Long actingUserId) throws Exception {

        // Validate manual fields
        if (req.getType() == DepositType.BANK_TRANSFER_OR_MFS) {
            if (req.getDepositBankId() == null || req.getRemarks() == null) {
                throw ServiceExceptions.duplicate("Bank transfer requires bank selection and remarks/reference.");
            }
        }
        if (req.getTransactionId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Transaction ID is required for deposits.");
        }

        // Reference: prefer client-provided reference if present; otherwise generate one.
        // Must be globally unique.
        String reference = req.getReference();

        if (depositRepo.existsReference(reference)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "A deposit with the same reference already exists.");
        }

        log.info("Creating deposit with reference: " + reference);

        if (reference == null || reference.isBlank()) {
            reference = referenceGeneratorService.nextReference("dp");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

//        Double convertedAmount = convertToUSD(user, req.getAmount()).doubleValue();
//        Double exchangeRate = Double.valueOf(currencyService.getExchangeRate("USD", user.getCurrency(), "OTHERS"));
//        log.info("Converted amount: " + convertedAmount + " and exchange rate: " + exchangeRate);
        // Create base WalletDeposit
        WalletDeposit deposit = WalletDeposit.builder()
                .userId(userId)
                .actingUserId(actingUserId)
                .type(req.getType())
                .status(DepositStatus.PENDING)
                .amount(req.getAmount())
                .exchangeRate(0.0)
                .remarks(req.getRemarks())
                .reference(reference)
                .attachment(attachmentFilename)
                // transactionId is a required client-supplied field for deposits; keep it as provided.
                .transactionId(req.getTransactionId())
                .createdAt(UserDateTimeUtil.now())
                .exchangedAmount(0.0)
                .currency(req.getCurrency())
                .depositDate(req.getDepositDate())
                .build();

        // Cheque extra fields
        if (req.getType() == DepositType.CHEQUE) {
            deposit.setChequeNo(req.getChequeNo());
            deposit.setChequeBank(req.getChequeBank());
            deposit.setChequeIssueDate(req.getChequeIssueDate());
        }

        // Set deposit bank if given
        if (req.getDepositBankId() != null) {
            deposit.setDepositBank(bankRepo.findById(req.getDepositBankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Deposit bank")));
        }

        // Save deposit record first (no transaction created yet – transaction is only saved on approval)
        try {
            deposit = depositRepo.save(deposit);


            // Notify all admin users about the new deposit request
            notifyAdminsAboutNewDeposit(deposit, user);

        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Deposit with this reference already exists.");
        }

        return mapToResponse(deposit);
    }

    private BigDecimal convertToUSD(User user, Double amount) {
        String userCurrency = user.getCurrency() != null
                ? user.getCurrency()
                : "USD";

        Double converted = currencyService.convertCurrency(
                String.valueOf(amount),
                userCurrency,
                "USD",
                "OTHERS"
        );

        if (converted == null || converted <= 0) {
            throw ServiceExceptions.duplicate("Currency conversion failed");
        }

        return BigDecimal.valueOf(converted);
    }

    public void instantDeposit(Long userId, Double amount, String remarks) {
        // Create a new deposit with status PENDING
        try {
            WalletDeposit deposit = WalletDeposit.builder()
                    .userId(userId)
                    .type(DepositType.INSTANT)
                    .status(DepositStatus.PENDING)
                    .amount(amount)
                    .remarks(remarks)
                    .reference(referenceGeneratorService.nextReference("dp"))
                    .createdAt(UserDateTimeUtil.now())
                    .build();

            depositRepo.save(deposit);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Deposit with this reference already exists.");
        }
    }

    public CheckoutSessionResponse createInstantDepositCheckoutSession(Long userId, Double amount, String remarks) {
        try {
            // Get Stripe credentials
            Optional<StripeCredentials> stripeCredOpt = stripeCredRepository.findById(1L);
            if (stripeCredOpt.isEmpty()) {
                throw new RuntimeException("Stripe credentials not configured");
            }

            StripeCredentials stripeCreds = stripeCredOpt.get();
            Stripe.apiKey = stripeCreds.getSecretKey();

            // Create a new deposit with status PENDING
            WalletDeposit deposit = WalletDeposit.builder()
                    .userId(userId)
                    .type(DepositType.BANK_TRANSFER_OR_MFS)
                    .status(DepositStatus.PENDING)
                    .amount(amount)
                    .remarks(remarks)
                    .reference(referenceGeneratorService.nextReference("dp"))
                    .createdAt(UserDateTimeUtil.now())
                    .build();

            depositRepo.save(deposit);

            // Convert amount to cents for Stripe (assuming USD)
            long amountInCents = Math.round(amount * 100);

            // Create Stripe checkout session
            String successUrl = domain + "/payment/result?status=success&session_id={CHECKOUT_SESSION_ID}&deposit_ref=" + deposit.getReference()
                    + "&amount=" + amount + "&val_id={CHECKOUT_SESSION_ID}&tran_id=" + deposit.getTransactionId();
            String cancelUrl = domain + "/payment/result?status=failed&deposit_ref=" + deposit.getReference();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Wallet Deposit")
                                                                    .setDescription("Instant wallet deposit - Ref: " + deposit.getReference())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("deposit_reference", deposit.getReference()) // optional, for session tracking
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("deposit_id", deposit.getId().toString())
                    // ✅ NEW: attach metadata to the PaymentIntent too
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .putMetadata("deposit_reference", deposit.getReference())
                                    .putMetadata("user_id", userId.toString())
                                    .putMetadata("deposit_id", deposit.getId().toString())
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            return CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .depositReference(deposit.getReference())
                    .amount(amount)
                    .status("pending")
                    .build();

        } catch (StripeException e) {
            throw ServiceExceptions.duplicate("Failed to create Stripe checkout session: " + e.getMessage());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Deposit with this reference already exists.");
        }
    }

    public Page<WalletDepositResponse> getPendingDeposits(String currencyCode, int page, int size) {
        return getPendingDeposits(currencyCode, page, size, null, null);
    }

    public Page<WalletDepositResponse> getPendingDeposits(String currencyCode, int page, int size,
                                                           LocalDate from, LocalDate to) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Currency currency = Currency.getIndexFromCode(currencyCode);

        Specification<WalletDeposit> spec = WalletDepositSpec.hasStatus(DepositStatus.PENDING);
        if (currency != null) spec = spec.and(WalletDepositSpec.hasCurrency(currency));
        if (from != null)     spec = spec.and(WalletDepositSpec.createdAfter(from));
        if (to != null)       spec = spec.and(WalletDepositSpec.createdBefore(to));

        return mapDepositPage(depositRepo.findAll(spec, pageRequest), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletDepositResponse approveOrReject(Long depositId, DepositStatus status, Long adminId, String adminRemarks) {
        WalletDeposit deposit = depositRepo.findById(depositId).orElseThrow(() -> new ResourceNotFoundException("Deposit"));

        if (deposit.getStatus() != DepositStatus.PENDING) {
            throw ServiceExceptions.insufficientBalance("Deposit is not in pending status");
        }

        deposit.setStatus(status);
        deposit.setApprovedAt(UserDateTimeUtil.now());
        deposit.setApprovedBy(adminId);
        if (adminRemarks != null) {
            deposit.setRemarks(adminRemarks);
        }
        depositRepo.save(deposit);
        if (status == DepositStatus.APPROVED) {
            // Create the transaction only now, at approval time
            User depositUser = userRepo.findById(deposit.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            Transaction txn = Transaction.builder()
                    .type(deposit.getType().name())
                    .amount(deposit.getAmount())
                    .currency(depositUser.getCurrency())
                    .convertedAmount(String.valueOf(0.0))
                    .exchangeRate(0.0)
                    .description(deposit.getType() + "-Ref: " + deposit.getReference())
                    .userId(deposit.getUserId())
                    .createdBy(String.valueOf(adminId))
                    .createdAt(UserDateTimeUtil.now())
                    .sourceType(TransactionSourceType.DEPOSIT.name())
                    .sourceId(deposit.getId())
                    .reference(deposit.getReference())
                    .active(true)
                    .build();
            transactionRepo.saveAndFlush(txn);
        }

        if (status == DepositStatus.APPROVED) {
            switch (deposit.getType()) {
                case CASH:
                case BANK_DEPOSIT:
                case CHEQUE:
                case BANK_TRANSFER_OR_MFS:
                case INSTANT:
                    // NEW: Simply add full deposit to wallet - it will bring negative balance toward zero/positive
                    // No credit replenishment needed - the wallet balance itself goes negative with credit limit system
                    addToWalletBalance(deposit.getUserId(), deposit.getAmount(),
                            deposit.getType().name(), "WalletService", deposit.getId(), "DEPOSIT", adminId);

                    try {
                        notificationHelper.sendPaymentSuccess(
                                deposit.getUserId(),
                                deposit.getAmount().toString(),
                                deposit.getCurrency() != null ? deposit.getCurrency().name() : "USD",
                                deposit.getTransactionId() != null ? deposit.getTransactionId() : deposit.getReference()
                        );
                    } catch (Exception e) {
                        log.warn("Could not send payment success notification for deposit ref {}: {}",
                                deposit.getReference(), e.getMessage());
                    }
                    bankLedgerService.recordAgentDepositApproval(deposit, adminId);
                    break;
                case WITHDRAWAL:
                    // Optionally: check for sufficient balance before approval!
                    deductFromWalletBalance(deposit.getUserId(), deposit.getAmount(),
                            "WITHDRAWAL", "WalletService", deposit.getId(), "DEPOSIT", adminId);
                    try {
                        notificationHelper.sendCustomNotification(
                                deposit.getUserId(),
                                NotificationType.ACCOUNT_UPDATE,
                                NotificationPriority.MEDIUM,
                                "Withdrawal Approved",
                                "Your withdrawal request for " + deposit.getAmount() + " " + (deposit.getCurrency() != null ? deposit.getCurrency().name() : "USD") + " has been approved.",
                                "/dashboard/transaction-history",
                                "View Wallet"
                        );
                    } catch (Exception e) {
                        log.warn("Could not send withdrawal notification for deposit ref {}: {}",
                                deposit.getReference(), e.getMessage());
                    }
                    break;
//                case REFUND: need to implement refund logic
                default:
                    // No action needed for cash deposits
                    break;

            }
        }

        activityAdminAuditSupport.logDepositDecision(
                depositId,
                deposit.getUserId(),
                status,
                deposit.getAmount(),
                adminRemarks);

        return mapToResponse(deposit);
    }

    public WalletDepositResponse createRefund(Long userId, Double amount, String remarks, String attachmentFilename) {
        WalletDeposit refund = WalletDeposit.builder().userId(userId).type(DepositType.REFUND).status(DepositStatus.PENDING).amount(amount).remarks(remarks).reference(referenceGeneratorService.nextReference("rf")).attachment(attachmentFilename).createdAt(UserDateTimeUtil.now()).build();

        depositRepo.save(refund);
        return mapToResponse(refund);
    }

    @Transactional
    public void addToWalletBalance(Long userId, Double amount) {
        updateWalletBalance(userId, amount, true, null, "WalletService", null, null, null);
    }

    @Transactional
    public void addToWalletBalance(Long userId, Double amount,
                                   String reason, String source, Long referenceId, String referenceType, Long performedBy) {
        updateWalletBalance(userId, amount, true, reason, source, referenceId, referenceType, performedBy);
    }

    @Transactional
    public void deductFromWalletBalance(Long userId, Double amount) {
        updateWalletBalance(userId, amount, false, null, "WalletService", null, null, null);
    }

    @Transactional
    public void deductFromWalletBalance(Long userId, Double amount,
                                        String reason, String source, Long referenceId, String referenceType, Long performedBy) {
        updateWalletBalance(userId, amount, false, reason, source, referenceId, referenceType, performedBy);
    }

    private void updateWalletBalance(Long userId, Double amount, boolean isCredit,
                                     String reason, String source, Long referenceId, String referenceType, Long performedBy) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Amount must be greater than zero");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        User walletUser = CreditLimitValidatorService.resolveWalletUser(user);

        double currentBalance = walletUser.getBalance() != null ? walletUser.getBalance() : 0.0;
        double newBalance = isCredit
                ? BigDecimal.valueOf(currentBalance).add(BigDecimal.valueOf(amount)).doubleValue()
                : BigDecimal.valueOf(currentBalance).subtract(BigDecimal.valueOf(amount)).doubleValue();

        // Negative balance is allowed - credit limit system allows wallet to go negative
        // The credit limit check is done in UserService.deductUserBalance()
        int updated = userRepo.updateBalance(walletUser.getId(), newBalance);
        if (updated == 0) {
            throw new ResourceNotFoundException("User");
        }

        balanceChangeHistoryRepository.save(BalanceChangeHistory.builder()
                .userId(walletUser.getId())
                .changeType(isCredit ? "CREDIT" : "DEBIT")
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .reason(reason)
                .source(source != null ? source : "WalletService")
                .referenceId(referenceId)
                .referenceType(referenceType)
                .performedBy(performedBy)
                .build());

        if (!isCredit) {
            creditLimitService.applyCreditUsageForDebit(
                    userId,
                    currentBalance,
                    amount,
                    reason != null ? reason : "Wallet debit",
                    performedBy);
        }
    }
    /**
     * Notify all admin users about a new deposit request
     */
    private void notifyAdminsAboutNewDeposit(WalletDeposit deposit, User user) {
        try {
            // Get all admins with ADMIN role
            List<AdminUser> admins = adminUserRepository.findAdminsByRoleSlug("admin");

            if (admins == null || admins.isEmpty()) {
                log.warn("No admin users found to notify about deposit request");
                return;
            }

            String userName = user.getFullName() != null ? user.getFullName() : user.getEmail();
            String currency = deposit.getCurrency() != null ? deposit.getCurrency().name() : "USD";

            for (AdminUser admin : admins) {
                if (admin != null && admin.getId() != null) {
                    try {
                        notificationHelper.sendCustomNotification(
                                admin.getId(),
                                NotificationType.ACCOUNT_UPDATE,
                                NotificationPriority.HIGH,
                                "New Deposit Request",
                                String.format("New deposit request from %s for %s %s. Reference: %s",
                                        userName,
                                        deposit.getAmount(),
                                        currency,
                                        deposit.getReference()),
                                "/pending-deposits",
                                "View Deposit"
                        );
                        log.info("Notified admin {} about deposit request {}", admin.getId(), deposit.getReference());
                    } catch (Exception e) {
                        log.error("Failed to notify admin {} about deposit request {}", admin.getId(), deposit.getReference(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify admins about deposit request {}", deposit.getReference(), e);
        }
    }

    private BusinessDto mapBusinessToDto(BusinessEntity business) {
        if (business == null) {
            return null;
        }

        BusinessDto businessDto = new BusinessDto();
        businessDto.setId(business.getId());
        businessDto.setCompanyName(business.getCompanyName());

        return businessDto;
    }

    public WalletDepositResponse mapToResponse1(WalletDeposit d) {
        return mapToResponse1(d, buildMappingContext(List.of(d)));
    }

    public WalletDepositResponse mapToResponse1(WalletDeposit d, DepositMappingContext context) {
        User user = Optional.ofNullable(d.getUserId())
                .map(context.usersById()::get)
                .orElse(null);

        String agencyName = Optional.ofNullable(d.getUserId())
                .map(context.agencyNameByUserId()::get)
                .orElse(null);
        String name = agencyName != null ? agencyName : Optional.ofNullable(user).map(User::getFullName).orElse(null);

        return WalletDepositResponse.builder()
                .id(d.getId())
                .reference(d.getReference())
                .type(d.getType())
                .status(d.getStatus())
                .amount(d.getAmount())
                .exchangeRate(d.getExchangeRate())
                .transactionId(d.getTransactionId())
                .remarks(d.getRemarks())
                .attachment(d.getAttachment())
                .chequeNo(d.getChequeNo())
                .chequeBank(d.getChequeBank())
                .exchangedAmount(d.getExchangedAmount())
                .chequeIssueDate(d.getChequeIssueDate())
                .depositBank(DepositBankMapper.resolve(d))
                .createdAt(timestampMapper.createdAt(d))
                .approvedAt(timestampMapper.toRequestUserTime(d.getApprovedAt(), d.getCreatedTimeOffset()))
                .createdBy(Optional.ofNullable(d.getUserId()).map(String::valueOf).orElse(null))
                .createdByName(name)
                .agencyId(d.getUserId())
                .agencyName(agencyName)
                .currency(d.getCurrency() != null ? d.getCurrency() : Currency.USD)
                .depositDate(d.getDepositDate())
                .build();
    }

    public WalletDepositResponse mapToResponse(WalletDeposit d) {
        return mapToResponse(d, buildMappingContext(List.of(d)));
    }

    public WalletDepositResponse mapToResponse(WalletDeposit d, DepositMappingContext context) {
        AdminUser approver = Optional.ofNullable(d.getApprovedBy())
                .map(context.adminUsersById()::get)
                .orElse(null);

        User user = Optional.ofNullable(d.getUserId())
                .map(context.usersById()::get)
                .orElse(null);

        String agencyName = Optional.ofNullable(d.getUserId())
                .map(context.agencyNameByUserId()::get)
                .orElse(null);
        String name = agencyName != null ? agencyName : Optional.ofNullable(user).map(User::getFullName).orElse(null);

        return WalletDepositResponse.builder()
                .id(d.getId())
                .reference(d.getReference())
                .type(d.getType())
                .status(d.getStatus())
                .amount(d.getAmount())
                .exchangeRate(d.getExchangeRate())
                .transactionId(d.getTransactionId())
                .remarks(d.getRemarks())
                .attachment(d.getAttachment())
                .chequeNo(d.getChequeNo())
                .chequeBank(d.getChequeBank())
                .exchangedAmount(d.getExchangedAmount())
                .chequeIssueDate(d.getChequeIssueDate())
                .depositBank(DepositBankMapper.resolve(d))
                .createdAt(timestampMapper.createdAt(d))
                .approvedAt(timestampMapper.toRequestUserTime(d.getApprovedAt(), d.getCreatedTimeOffset()))
                .approvedBy((d.getApprovedAt() != null && approver != null) ? approver.getFullName() : null)
                .createdBy(Optional.ofNullable(d.getUserId()).map(String::valueOf).orElse(null))
                .createdByName(name)
                .currency(d.getCurrency() != null ? d.getCurrency() : Currency.USD)
                .depositDate(d.getDepositDate())
                .build();
    }

    public List<WalletStatementResponse> getStatement(Long userId, LocalDate from, LocalDate to) {
        Specification<WalletDeposit> spec = (root, query, cb) -> cb.equal(root.get("userId"), userId);
        Specification<WalletDeposit> dateSpec = OffsetAwareDateSpec.createdAtInUserRange(
                from, to, "createdAt", "createdTimeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }

        List<WalletDeposit> txns = depositRepo.findAll(spec, Sort.by("createdAt").ascending());
        double balance = 0.0;
        List<WalletStatementResponse> result = new ArrayList<>();
        // filter out non-approved transactions
        txns = txns.stream().filter(txn -> txn.getStatus() == DepositStatus.APPROVED).toList();

        for (WalletDeposit txn : txns) {
            Double withdrawal = null, deposit = null;
            if (txn.getStatus() == DepositStatus.APPROVED) {
                switch (txn.getType()) {
                    case BANK_DEPOSIT:
                    case BANK_TRANSFER_OR_MFS:
                    case CHEQUE:
                    case CASH:
                    case INSTANT:
//                    case REFUND:
                        deposit = txn.getAmount();
                        balance += deposit;
                        break;
                    case WITHDRAWAL:
                        withdrawal = txn.getAmount();
                        balance -= withdrawal;
                        break;
                }
            }
            result.add(
                    WalletStatementResponse.builder().
                            date(timestampMapper.createdAt(txn)).
                            transactionType(txn.getType().name())
                            .transactionCode(getTxnCode(txn.getType()))
                            .referenceNumber(txn.getReference())
                            .notes(txn.getRemarks()).withdrawal(withdrawal).
                            deposit(deposit).balance(balance)
                            .build());
        }
        return result;
    }

    private String getTxnCode(DepositType type) {
        return switch (type) {
            case BANK_DEPOSIT, BANK_TRANSFER_OR_MFS, CHEQUE, CASH, INSTANT ->
                "DB";
            case WITHDRAWAL ->
                "WD";
            case REFUND ->
                "RF";
            case ADMIN_CHARGE ->
                "AC";
            default ->
                "UN"; // Unknown or unhandled type
        };
    }

    public TodayDepositsSummaryResponse getSumOfTodayDepositsByDepositTypes() {
        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        List<DepositType> depositTypes = List.of(DepositType.BANK_DEPOSIT, DepositType.BANK_TRANSFER_OR_MFS, DepositType.CHEQUE, DepositType.CASH);

        List<WalletDeposit> deposits = findDepositsByTypesInUserDateRange(depositTypes, today, today);

        List<WalletDeposit> approvedDeposits = deposits.stream()
                .filter(deposit -> deposit.getStatus() == DepositStatus.APPROVED)
                .toList();

        Double totalSum = approvedDeposits.stream().mapToDouble(WalletDeposit::getAmount).sum();
        long approvedCount = approvedDeposits.size();

        // Collect unique user IDs from approved deposits
        Set<Long> userIds = approvedDeposits.stream()
                .map(WalletDeposit::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Batch-fetch users and build a userId -> currency map
        Map<Long, String> userCurrencyMap = userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u.getCurrency() != null ? u.getCurrency() : "UNKNOWN"
                ));

        // Group by user currency: sum amounts and count per currency
        Map<String, double[]> grouped = new HashMap<>();
        for (WalletDeposit dep : approvedDeposits) {
            String currency = dep.getUserId() != null
                    ? userCurrencyMap.getOrDefault(dep.getUserId(), "UNKNOWN")
                    : "UNKNOWN";
            grouped.computeIfAbsent(currency, k -> new double[]{0.0, 0.0});
            grouped.get(currency)[0] += dep.getAmount(); // sum
            grouped.get(currency)[1] += 1;               // count
        }

        List<DepositCurrencySummaryDto> byCurrency = grouped.entrySet().stream()
                .map(e -> DepositCurrencySummaryDto.builder()
                        .currency(e.getKey())
                        .totalAmount(e.getValue()[0])
                        .count((long) e.getValue()[1])
                        .build())
                .sorted(Comparator.comparing(DepositCurrencySummaryDto::getCurrency))
                .toList();

        return TodayDepositsSummaryResponse.builder()
                .todayDeposits(totalSum)
                .todayApprovedDeposits(approvedCount)
                .byCurrency(byCurrency)
                .build();
    }

    public TodayDepositsSummaryResponse getSumOfLast7DaysDepositsByDepositTypes() {
        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        LocalDate sevenDaysAgo = today.minusDays(6); // inclusive: today + 6 previous days = 7 days
        List<DepositType> depositTypes = List.of(DepositType.BANK_DEPOSIT, DepositType.BANK_TRANSFER_OR_MFS, DepositType.CHEQUE, DepositType.CASH);

        List<WalletDeposit> deposits = findDepositsByTypesInUserDateRange(depositTypes, sevenDaysAgo, today);

        List<WalletDeposit> approvedDeposits = deposits.stream()
                .filter(deposit -> deposit.getStatus() == DepositStatus.APPROVED)
                .toList();

        Double totalSum = approvedDeposits.stream().mapToDouble(WalletDeposit::getAmount).sum();
        long approvedCount = approvedDeposits.size();

        // Batch-fetch users and build userId -> currency map
        Set<Long> userIds = approvedDeposits.stream()
                .map(WalletDeposit::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> userCurrencyMap = userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u.getCurrency() != null ? u.getCurrency() : "UNKNOWN"
                ));

        // Group by user currency
        Map<String, double[]> grouped = new HashMap<>();
        for (WalletDeposit dep : approvedDeposits) {
            String currency = dep.getUserId() != null
                    ? userCurrencyMap.getOrDefault(dep.getUserId(), "UNKNOWN")
                    : "UNKNOWN";
            grouped.computeIfAbsent(currency, k -> new double[]{0.0, 0.0});
            grouped.get(currency)[0] += dep.getAmount();
            grouped.get(currency)[1] += 1;
        }

        List<DepositCurrencySummaryDto> byCurrency = grouped.entrySet().stream()
                .map(e -> DepositCurrencySummaryDto.builder()
                        .currency(e.getKey())
                        .totalAmount(e.getValue()[0])
                        .count((long) e.getValue()[1])
                        .build())
                .sorted(Comparator.comparing(DepositCurrencySummaryDto::getCurrency))
                .toList();

        return TodayDepositsSummaryResponse.builder()
                .todayDeposits(totalSum)
                .todayApprovedDeposits(approvedCount)
                .byCurrency(byCurrency)
                .build();
    }

    private List<WalletDeposit> findDepositsByTypesInUserDateRange(
            List<DepositType> depositTypes, LocalDate from, LocalDate to) {
        Specification<WalletDeposit> spec = WalletDepositSpec.hasTypes(depositTypes);
        Specification<WalletDeposit> dateSpec = OffsetAwareDateSpec.createdAtInUserRange(
                from, to, "createdAt", "createdTimeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return depositRepo.findAll(spec);
    }

//    @Transactional
//    public Object createNgeniusWalletDeposit(PaymentRequestDto paymentRequestDto, String provider, Long authUserId, Long targetUserId) {

//        try {
//            Long currentUserid = paymentRequestDto.getUserId() == null ? targetUserId : paymentRequestDto.getUserId();
//            // Create wallet deposit
//            WalletDeposit deposit = WalletDeposit.builder()
//                    .userId(currentUserid)
//                    .actingUserId(authUserId)
//                    .type(DepositType.NGENIUS)
//                    .status(DepositStatus.INIT)
//                    .amount(paymentRequestDto.getAmount())
//                    .exchangeRate(paymentRequestDto.getExchangeRate())
//                    .reference(referenceGeneratorService.nextReference("dp"))
//                    .createdAt(UserDateTimeUtil.now())
//                    .exchangedAmount(paymentRequestDto.getExchangeRate())
//                    .currency(Currency.AED)
//                    .build();
//
//            depositRepo.save(deposit);
//
//            // Create transaction
//            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
//            transactionCreateDto.setCurrency(paymentRequestDto.getCurrency());
//            transactionCreateDto.setAmount(paymentRequestDto.getAmount());
//            transactionCreateDto.setConvertedAmount(String.valueOf(paymentRequestDto.getExchangeRate()));
//            transactionCreateDto.setType("DEPOSIT");
//            transactionCreateDto.setDescription("Deposit - Ref: " + deposit.getReference());
//            transactionCreateDto.setCreatedBy(String.valueOf(authUserId));
//            transactionCreateDto.setCreatedAt(UserDateTimeUtil.now());
//            transactionCreateDto.setUserId(currentUserid);
//
//            Transaction transaction = transactionService.createTransaction(transactionCreateDto, currentUserid);
//
//            // Update transaction id to WalletDeposit
//            deposit.setTransactionId(String.valueOf(transaction.getId()));
//            depositRepo.save(deposit);
//
//            // Create payment
//            Object obj = paymentService.createPayment(provider, currentUserid, paymentRequestDto);
//
//            Map<String, Object> result = (Map<String, Object>) obj;
//            Long paymentId = (Long) result.get("payment_id");
//
//            // update payment id to transaction
//            transaction.setPaymentId(paymentId);
//            transactionRepo.save(transaction);
//            return obj;
//        } catch (Exception exception) {
//            throw ServiceExceptions.insufficientBalance("Error: " + exception.getMessage());
//        }
//    }

    /**
     * Admin charges (debits) a user's wallet balance immediately.
     *
     * - Creates a {@link WalletDeposit} with type=ADMIN_CHARGE,
     * status=APPROVED. - Deducts the amount from the user's wallet balance. -
     * Records an active {@link Transaction} for the ledger/history.
     *
     * @param req charge details (userId, amount, currency, reason)
     * @param adminId the ID of the admin performing the charge
     * @return the saved deposit response
     */
    @Transactional(rollbackFor = Exception.class)
    public WalletDepositResponse adminCharge(AdminChargeRequest req, Long adminId) {
        // 1. Verify user exists
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        double chargeAmount = req.getAmount() != null ? req.getAmount() : 0.0;

        // 2. Verify sufficient balance (wallet + credit limit) before deducting
        boolean canOverrideBalance = hasAdminOverrideBalancePermission(adminId);
        double currentBalance = user.getBalance() != null ? user.getBalance() : 0.0;
        if (currentBalance < chargeAmount) {
            boolean hasSufficient = creditLimitValidatorService.hasSufficientBalance(
                    req.getUserId(), currentBalance, chargeAmount);
            if (!hasSufficient && !canOverrideBalance) {
                double availableBalance = creditLimitValidatorService.getAvailableBalance(req.getUserId(), currentBalance);
                throw ServiceExceptions.insufficientBalance("Insufficient balance and credit. Available balance (wallet + credit): "
                                + availableBalance + ", charge amount: " + chargeAmount);
            }
            log.info("Admin {} charging user {} amount {}. Override used: {}. Current balance: {}",
                    adminId, req.getUserId(), chargeAmount, canOverrideBalance, currentBalance);
        }

        // 3. Build and persist the WalletDeposit record (immediately APPROVED)
        String reference = referenceGeneratorService.nextReference("ac");
        WalletDeposit deposit = WalletDeposit.builder()
                .userId(req.getUserId())
                .actingUserId(adminId)
                .type(DepositType.ADMIN_CHARGE)
                .status(DepositStatus.APPROVED)
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .remarks(req.getReason())
                .reference(reference)
                .transactionId(reference) // use reference as internal txn id
                .exchangeRate(0.0)
                .exchangedAmount(0.0)
                .createdAt(UserDateTimeUtil.now())
                .approvedAt(UserDateTimeUtil.now())
                .approvedBy(adminId)
                .build();

        deposit = depositRepo.save(deposit);

        // 4. Deduct from user balance
        deductFromWalletBalance(req.getUserId(), req.getAmount(),
                "ADMIN_CHARGE", "WalletService", deposit.getId(), "DEPOSIT", adminId);

        // 5. Record an active transaction for ledger / history
        Transaction txn = Transaction.builder()
                .type(DepositType.ADMIN_CHARGE.name())
                .amount(req.getAmount())
                .currency(req.getCurrency().name())
                .convertedAmount(String.valueOf(req.getAmount()))
                .exchangeRate(0.0)
                .description("Admin charge – " + req.getReason() + " | Ref: " + reference)
                .userId(req.getUserId())
                .createdBy("ADMIN:" + adminId)
                .createdAt(UserDateTimeUtil.now())
                .sourceType(TransactionSourceType.DEPOSIT.name())
                .sourceId(deposit.getId())
                .reference(reference)
                .active(true)
                .build();

        transactionRepo.saveAndFlush(txn);

        log.info("Admin {} charged user {} an amount of {} {}. Ref: {}",
                adminId, req.getUserId(), req.getAmount(), req.getCurrency(), reference);

        activityAdminAuditSupport.logBalanceMovement(
                ActivityEventType.BALANCE_DEBIT,
                req.getUserId(),
                req.getAmount(),
                req.getCurrency() != null ? req.getCurrency().name() : null,
                req.getReason(),
                reference,
                "DEPOSIT",
                deposit.getId());

        // 6. Notify the user
        try {
            notificationHelper.sendCustomNotification(
                    req.getUserId(),
                    NotificationType.ACCOUNT_UPDATE,
                    NotificationPriority.HIGH,
                    "Wallet Charge",
                    String.format("An admin charge of %s %s has been applied to your account. Reason: %s",
                            req.getAmount(), req.getCurrency().name(), req.getReason()),
                    "/dashboard/transaction-history",
                    "View History"
            );
        } catch (Exception e) {
            log.warn("Could not send notification for admin charge ref {}: {}", reference, e.getMessage());
        }

        return mapToResponse(deposit);
    }

    /**
     * Admin directly credits a user's wallet with a given amount, attaching a
     * proof image URL. The deposit is immediately APPROVED and the user's
     * balance is credited right away.
     */
    @Transactional(rollbackFor = Exception.class)
    public WalletDepositResponse adminDeposit(AdminDepositRequest req, Long adminId) {
        // 1. Verify user exists
        long targetUserId = req.getMotherUserId();
        userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        // 2. Generate reference
        String reference = referenceGeneratorService.nextReference("ad");

        // 3. Build and persist the WalletDeposit record (immediately APPROVED)
        WalletDeposit deposit = WalletDeposit.builder()
                .userId(targetUserId)
                .actingUserId(adminId)
                .type(DepositType.CASH)
                .status(DepositStatus.APPROVED)
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .remarks(req.getRemarks())
                .reference(reference)
                .transactionId(reference)
                .attachment(req.getImageUrl())
                .exchangeRate(0.0)
                .exchangedAmount(0.0)
                .createdAt(UserDateTimeUtil.now())
                .approvedAt(UserDateTimeUtil.now())
                .approvedBy(adminId)
                .build();

        if (req.getDepositBankId() != null) {
            deposit.setDepositBank(bankRepo.findById(req.getDepositBankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Deposit bank", req.getDepositBankId())));
        }

        deposit = depositRepo.save(deposit);

        // 4. Credit user balance
        addToWalletBalance(targetUserId, req.getAmount(),
                "ADMIN_DEPOSIT", "WalletService", deposit.getId(), "DEPOSIT", adminId);

        // 5. Record an active transaction for ledger / history
        Transaction txn = Transaction.builder()
                .type(DepositType.CASH.name())
                .amount(req.getAmount())
                .currency(req.getCurrency().name())
                .convertedAmount(String.valueOf(req.getAmount()))
                .exchangeRate(0.0)
                .description("Admin deposit" + (req.getRemarks() != null ? " – " + req.getRemarks() : "") + " | Ref: " + reference)
                .userId(targetUserId)
                .createdBy("ADMIN:" + adminId)
                .createdAt(UserDateTimeUtil.now())
                .sourceType(TransactionSourceType.DEPOSIT.name())
                .sourceId(deposit.getId())
                .reference(reference)
                .active(true)
                .build();

        transactionRepo.saveAndFlush(txn);

        if (req.getDepositBankId() != null) {
            bankLedgerService.recordAdminDeposit(
                    req.getDepositBankId(),
                    deposit.getId(),
                    BigDecimal.valueOf(req.getAmount()),
                    req.getCurrency(),
                    reference,
                    req.getRemarks(),
                    adminId
            );
        }

        log.info("Admin {} deposited {} {} for user {}. Ref: {}",
                adminId, req.getAmount(), req.getCurrency(), targetUserId, reference);

        activityAdminAuditSupport.logBalanceMovement(
                ActivityEventType.BALANCE_CREDIT,
                targetUserId,
                req.getAmount(),
                req.getCurrency() != null ? req.getCurrency().name() : null,
                req.getRemarks(),
                reference,
                "DEPOSIT",
                deposit.getId());

        // 6. Notify the user
        try {
            notificationHelper.sendCustomNotification(
                    targetUserId,
                    NotificationType.ACCOUNT_UPDATE,
                    NotificationPriority.HIGH,
                    "Wallet Deposit",
                    String.format("A deposit of %s %s has been added to your account by admin.",
                            req.getAmount(), req.getCurrency().name()),
                    "/dashboard/transaction-history",
                    "View History"
            );
        } catch (Exception e) {
            log.warn("Could not send notification for admin deposit ref {}: {}", reference, e.getMessage());
        }

        return mapToResponse(deposit);
    }

    /**
     * Hard-deletes a transaction and reverses its wallet balance effect.
     * CREDIT transactions: deducts the amount (undoing the original credit).
     * DEBIT transactions: adds the amount back (undoing the original deduction).
     * Linked WalletDeposit is removed first, then the transaction row.
     *
     * @param transactionId the ID of the transaction to delete
     */
    @Transactional
    public void deleteTransaction(Long transactionId) {
        Transaction txn = transactionRepo.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        TransactionStatus status;
        try {
            status = DepositTypeUtil.determineStatus(txn.getType());
        } catch (Exception e) {
            throw ServiceExceptions.business("Cannot determine transaction status");
        }

        double amount = txn.getAmount() != null ? txn.getAmount() : 0.0;
        Long userId = txn.getUserId();

        if (TransactionSourceType.DEPOSIT.name().equals(txn.getSourceType()) && txn.getSourceId() != null) {
            depositRepo.findById(txn.getSourceId()).ifPresent(deposit -> {
                depositRepo.delete(deposit);
                depositRepo.flush();
            });
        } else if (txn.getReference() != null) {
            depositRepo.findByReference(txn.getReference()).ifPresent(deposit -> {
                depositRepo.delete(deposit);
                depositRepo.flush();
            });
        }

        transactionRepo.delete(txn);
        // Flush before balance update: updateBalance uses @Modifying(clearAutomatically = true)
        // which clears the persistence context and would drop a pending delete.
        transactionRepo.flush();

        if (userId != null && amount > 0) {
            User actor = userRepo.findById(userId).orElse(null);
            if (actor != null) {
                User walletUser = CreditLimitValidatorService.resolveWalletUser(actor);
                double currentBalance = walletUser.getBalance() != null ? walletUser.getBalance() : 0.0;

                if (status == TransactionStatus.CREDIT) {
                    userRepo.updateBalance(walletUser.getId(), currentBalance - amount);
                    removeMatchingBalanceHistoryCredits(walletUser.getId(), txn);
                } else {
                    userRepo.updateBalance(walletUser.getId(), currentBalance + amount);
                    removeMatchingBalanceHistoryDebits(walletUser.getId(), txn, amount);
                }
            }
        }

        log.info("Deleted {} transaction {} (amount={}, userId={}) and silently adjusted balance.",
                status, transactionId, amount, userId);
    }

    private void removeMatchingBalanceHistoryDebits(Long walletUserId, Transaction txn, double amount) {
        List<Long> historyIds = new ArrayList<>();

        if (TransactionSourceType.BOOKING.name().equals(txn.getSourceType()) && txn.getSourceId() != null) {
            balanceChangeHistoryRepository
                    .findByUserIdAndReferenceTypeAndReferenceIdAndChangeType(
                            walletUserId, "BOOKING", txn.getSourceId(), "DEBIT")
                    .forEach(row -> historyIds.add(row.getId()));
        }

        if (historyIds.isEmpty()) {
            balanceChangeHistoryRepository
                    .findFirstByUserIdAndReferenceTypeAndReferenceIdIsNullAndChangeTypeAndAmount(
                            walletUserId, "BOOKING", "DEBIT", amount)
                    .ifPresent(row -> historyIds.add(row.getId()));
        }

        if (!historyIds.isEmpty()) {
            balanceChangeHistoryRepository.deleteAllById(historyIds);
        }
    }

    private void removeMatchingBalanceHistoryCredits(Long walletUserId, Transaction txn) {
        List<Long> historyIds = new ArrayList<>();

        if (TransactionSourceType.DEPOSIT.name().equals(txn.getSourceType()) && txn.getSourceId() != null) {
            balanceChangeHistoryRepository
                    .findByUserIdAndReferenceTypeAndReferenceIdAndChangeType(
                            walletUserId, "DEPOSIT", txn.getSourceId(), "CREDIT")
                    .forEach(row -> historyIds.add(row.getId()));
        }

        if (!historyIds.isEmpty()) {
            balanceChangeHistoryRepository.deleteAllById(historyIds);
        }
    }

    private boolean hasAdminOverrideBalancePermission(Long adminId) {
        if (adminId == null || !adminUserRepository.existsById(adminId)) {
            return false;
        }
        return roleAssignmentRepository.findByEntityTypeAndEntityId("ADMIN", adminId)
                .map(ra -> ra.getRole() != null && ra.getRole().getPermissions() != null
                        && ra.getRole().getPermissions().stream()
                        .anyMatch(p -> p.getSlug() != null && "override-balance".equalsIgnoreCase(p.getSlug())))
                .orElse(false);
    }
}
