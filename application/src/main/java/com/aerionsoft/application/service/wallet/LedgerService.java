package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.context.UserTimezoneContext;
import com.aerionsoft.application.util.FilterRangeUtil;
import com.aerionsoft.application.util.TimestampMapper;

import com.aerionsoft.application.dto.ledger.LedgerPageResponse;
import com.aerionsoft.application.dto.ledger.LedgerPaginatedResponse;
import com.aerionsoft.application.dto.ledger.LedgerResponse;
import com.aerionsoft.application.dto.wallet.SourceEnrichment;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.CreditLimitHistory;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.util.DepositTypeUtil;
import com.aerionsoft.application.enums.wallet.TransactionStatus;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.wallet.CreditLimitHistoryRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CreditLimitHistoryRepository creditLimitHistoryRepository;
    private final BusinessRepository businessRepository;
    private final AdminUserRepository adminUserRepository;
    private final TransactionEnrichmentService transactionEnrichmentService;
    private final DepositTransactionSourceResolver depositTransactionSourceResolver;
    private final TimestampMapper timestampMapper;

    public LedgerService(TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         CreditLimitHistoryRepository creditLimitHistoryRepository,
                         BusinessRepository businessRepository,
                         AdminUserRepository adminUserRepository,
                         TransactionEnrichmentService transactionEnrichmentService,
                         DepositTransactionSourceResolver depositTransactionSourceResolver,
                         TimestampMapper timestampMapper) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.creditLimitHistoryRepository = creditLimitHistoryRepository;
        this.businessRepository = businessRepository;
        this.adminUserRepository = adminUserRepository;
        this.transactionEnrichmentService = transactionEnrichmentService;
        this.depositTransactionSourceResolver = depositTransactionSourceResolver;
        this.timestampMapper = timestampMapper;
    }

    /**
     * Get ledger entries for a user with detailed deposit/booking info,
     * merged with credit limit history for the user's business.
     */
    public Page<LedgerResponse> getLedger(Long userId, int page, int size, TransactionStatus transactionStatus,
                                          LocalDate from, LocalDate to) {
        List<LedgerResponse> ledgerEntries = buildLedgerEntriesForUser(userId, transactionStatus, from, to);
        return paginateLedger(ledgerEntries, page, size);
    }

    /**
     * Same ledger as {@link #getLedger(Long, int, int, TransactionStatus, LocalDate, LocalDate)} plus
     * {@code openingBalance}: net of all wallet transactions and credit-limit history strictly before
     * the filter window (start of {@code from} when set, otherwise before the oldest row in the
     * filtered result).
     */
    public LedgerPageResponse getLedgerWithOpeningBalance(Long userId,
                                                          TransactionStatus transactionStatus,
                                                          LocalDate from, LocalDate to) {
        List<LedgerResponse> ledgerEntries = buildLedgerEntriesForUser(userId, transactionStatus, from, to);

        // Sort ASC
        ledgerEntries.sort(Comparator.comparing(LedgerResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Instant boundary = openingBalanceBoundary(from, ledgerEntries);
        double opening = boundary != null ? computeNetBalanceBefore(userId, boundary) : 0.0;

        return LedgerPageResponse.builder()
                .openingBalance(opening)
                .entries(ledgerEntries)
                .build();
    }

    /**
     * Same as {@link #getLedger(Long, int, int, TransactionStatus, LocalDate, LocalDate)} but also
     * computes {@code openingBalance}: net before the filter window start.
     */
    public LedgerPaginatedResponse getLedgerPagedWithOpeningBalance(Long userId, int page, int size,
                                                                    TransactionStatus transactionStatus,
                                                                    LocalDate from, LocalDate to) {
        List<LedgerResponse> all = buildLedgerEntriesForUser(userId, transactionStatus, from, to);

        // Sort ASC to find oldest entry (needed when from is null)
        all.sort(Comparator.comparing(LedgerResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Instant boundary = openingBalanceBoundary(from, all);
        double opening = boundary != null ? computeNetBalanceBefore(userId, boundary) : 0.0;

        Page<LedgerResponse> pagedResult = paginateLedger(all, page, size);
        return LedgerPaginatedResponse.builder()
                .openingBalance(opening)
                .data(pagedResult)
                .build();
    }

    /**
     * Same as {@link #getAllLedger(int, int, TransactionStatus, LocalDate, LocalDate)} but also
     * computes {@code openingBalance}: net of ALL transactions and credit-limit rows before
     * the filter window start.
     */
    public LedgerPaginatedResponse getAllLedgerPagedWithOpeningBalance(int page, int size,
                                                                      TransactionStatus transactionStatus,
                                                                      LocalDate from, LocalDate to) {
        List<LedgerResponse> all = buildLedgerEntriesForAll(transactionStatus, from, to);

        // Sort ASC to find oldest entry
        all.sort(Comparator.comparing(LedgerResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Instant boundary = openingBalanceBoundary(from, all);
        double opening = boundary != null ? computeAllNetBalanceBefore(boundary) : 0.0;

        Page<LedgerResponse> pagedResult = paginateLedger(all, page, size);
        return LedgerPaginatedResponse.builder()
                .openingBalance(opening)
                .data(pagedResult)
                .build();
    }

    /** Net of ALL transactions and credit-limit rows strictly before {@code boundary}. */
    private double computeAllNetBalanceBefore(Instant boundary) {
        double sum = 0.0;
        for (Transaction t : transactionRepository.findAll(Sort.by("createdAt").ascending())) {
            Instant at = timestampMapper.toStoredInstant(t.getCreatedAt(), t.getCreatedTimeOffset());
            if (at != null && at.isBefore(boundary)) {
                sum += netEffectOnBalance(t);
            }
        }
        for (CreditLimitHistory h : creditLimitHistoryRepository.findAll(Sort.by("createdAt").ascending())) {
            Instant at = timestampMapper.toStoredInstant(h.getCreatedAt(), h.getCreatedTimeOffset());
            if (at != null && at.isBefore(boundary)) {
                sum += netEffectOnBalance(h);
            }
        }
        return sum;
    }

    private List<LedgerResponse> buildLedgerEntriesForUser(Long userId, TransactionStatus transactionStatus,
                                                           LocalDate from, LocalDate to) {
        List<Transaction> transactions;
        if (transactionStatus != null) {
            List<String> types = getTransactionTypesByStatus(transactionStatus);
            transactions = transactionRepository.findAllByUserIdAndTypeInUnpaged(userId, types);
        } else {
            transactions = transactionRepository.findAllByUserIdUnpaged(userId);
        }

        // Apply date filter on transactions (offset-aware)
        if (from != null || to != null) {
            transactions = transactions.stream()
                    .filter(t -> timestampMapper.isInUserDateRange(
                            t.getCreatedAt(), t.getCreatedTimeOffset(), from, to))
                    .toList();
        }

        List<LedgerResponse> ledgerEntries = new ArrayList<>(
                mapTransactionsToLedger(transactions)
        );

        // Merge credit limit history for the user's business
        Long businessId = getUserBusinessId(userId);
        if (businessId != null) {
            List<CreditLimitHistory> creditHistory =
                    creditLimitHistoryRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);

            if (from != null || to != null) {
                creditHistory = creditHistory.stream()
                        .filter(h -> timestampMapper.isInUserDateRange(
                                h.getCreatedAt(), h.getCreatedTimeOffset(), from, to))
                        .toList();
            }

            if (transactionStatus != null) {
                creditHistory = creditHistory.stream()
                        .filter(h -> matchesCreditLimitStatus(h, transactionStatus))
                        .toList();
            }

            creditHistory.stream()
                    .map(this::mapCreditLimitToLedgerResponse)
                    .forEach(ledgerEntries::add);
        }

        // Sort merged list by transactionId ASC (nulls last for credit-limit entries)
        ledgerEntries.sort(Comparator.comparing(LedgerResponse::getTransactionId,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return ledgerEntries;
    }

    private Page<LedgerResponse> paginateLedger(List<LedgerResponse> ledgerEntries, int page, int size) {
        int totalElements = ledgerEntries.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        List<LedgerResponse> pagedEntries = start < totalElements
                ? ledgerEntries.subList(start, end)
                : new ArrayList<>();
        return new PageImpl<>(pagedEntries, PageRequest.of(page, size), totalElements);
    }

    /**
     * Cutoff for opening balance: start of {@code from} in {@link UserTimezoneContext}, or the
     * earliest visible entry instant when {@code from} is unset.
     */
    private Instant openingBalanceBoundary(LocalDate from, List<LedgerResponse> entries) {
        Instant fromStart = FilterRangeUtil.userDateRange(from, null).start();
        if (fromStart != null) {
            return fromStart;
        }
        return entries.stream()
                .map(LedgerResponse::getCreatedAt)
                .filter(Objects::nonNull)
                .map(dt -> dt.atZone(UserTimezoneContext.getZoneId()).toInstant())
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * Net balance from all active user transactions and business credit-limit rows with stored
     * instant strictly before {@code boundary}.
     */
    private double computeNetBalanceBefore(Long userId, Instant boundary) {
        double sum = 0.0;
        for (Transaction t : transactionRepository.findAllByUserIdUnpaged(userId)) {
            Instant at = timestampMapper.toStoredInstant(t.getCreatedAt(), t.getCreatedTimeOffset());
            if (at != null && at.isBefore(boundary)) {
                sum += netEffectOnBalance(t);
            }
        }
        Long businessId = getUserBusinessId(userId);
        if (businessId != null) {
            for (CreditLimitHistory h : creditLimitHistoryRepository.findByBusinessIdOrderByCreatedAtDesc(businessId)) {
                Instant at = timestampMapper.toStoredInstant(h.getCreatedAt(), h.getCreatedTimeOffset());
                if (at != null && at.isBefore(boundary)) {
                    sum += netEffectOnBalance(h);
                }
            }
        }
        return sum;
    }

    private double netEffectOnBalance(Transaction t) {
        if (t.getCreatedAt() == null) {
            return 0.0;
        }
        TransactionStatus status = DepositTypeUtil.determineStatus(t.getType());
        double amt = t.getAmount() != null ? t.getAmount() : 0.0;
        if (status == TransactionStatus.CREDIT) {
            return amt;
        } else if (status == TransactionStatus.DEBIT) {
            return -amt;
        }
        return 0.0;
    }

    private double netEffectOnBalance(CreditLimitHistory h) {
        if (h.getCreatedAt() == null) {
            return 0.0;
        }
        if (h.getStatus() == CreditLimitStatus.DEBIT) {
            return h.getAmount() != null ? -h.getAmount().doubleValue() : 0.0;
        }
        return h.getAmount() != null ? h.getAmount().doubleValue() : 0.0;
    }

    public Page<LedgerResponse> getLedger(Long userId, int page, int size, TransactionStatus transactionStatus) {
        return getLedger(userId, page, size, transactionStatus, null, null);
    }

    /**
     * Get ledger entries for a user within a date range, merged with credit limit history.
     */
    public List<LedgerResponse> getLedgerByDateRange(Long userId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionRepository.findAllByUserIdUnpaged(userId).stream()
                .filter(t -> timestampMapper.isInUserDateRange(
                        t.getCreatedAt(), t.getCreatedTimeOffset(), from, to))
                .sorted(Comparator.comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<LedgerResponse> ledgerEntries = new ArrayList<>(mapTransactionsToLedger(transactions));

        Long businessId = getUserBusinessId(userId);
        if (businessId != null) {
            creditLimitHistoryRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                    .filter(h -> timestampMapper.isInUserDateRange(
                            h.getCreatedAt(), h.getCreatedTimeOffset(), from, to))
                    .map(this::mapCreditLimitToLedgerResponse)
                    .forEach(ledgerEntries::add);
        }

        // Sort by createdAt ASC
        ledgerEntries.sort(Comparator.comparing(LedgerResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        // Compute running balance
        double runningBalance = 0.0;
        for (LedgerResponse entry : ledgerEntries) {
            if (entry.getTransactionStatus() == TransactionStatus.CREDIT) {
                runningBalance += entry.getAmount() != null ? entry.getAmount() : 0.0;
            } else if (entry.getTransactionStatus() == TransactionStatus.DEBIT) {
                runningBalance -= entry.getAmount() != null ? entry.getAmount() : 0.0;
            }
            entry.setRunningBalance(runningBalance);
        }

        return ledgerEntries;
    }

    /**
     * Get all ledger entries (admin view), merged with all credit limit history.
     */
    public Page<LedgerResponse> getAllLedger(int page, int size, TransactionStatus transactionStatus) {
        return getAllLedger(page, size, transactionStatus, null, null);
    }

    public Page<LedgerResponse> getAllLedger(int page, int size, TransactionStatus transactionStatus,
                                             LocalDate from, LocalDate to) {
        List<LedgerResponse> ledgerEntries = buildLedgerEntriesForAll(transactionStatus, from, to);
        return paginateLedger(ledgerEntries, page, size);
    }

    private List<LedgerResponse> buildLedgerEntriesForAll(TransactionStatus transactionStatus,
                                                          LocalDate from, LocalDate to) {
        List<Transaction> transactions;
        if (transactionStatus != null) {
            List<String> types = getTransactionTypesByStatus(transactionStatus);
            transactions = transactionRepository.findAllByTypeInUnpaged(types);
        } else {
            transactions = transactionRepository.findAll(Sort.by("createdAt").descending());
        }

        // Apply date filter on transactions (offset-aware)
        if (from != null || to != null) {
            transactions = transactions.stream()
                    .filter(t -> timestampMapper.isInUserDateRange(
                            t.getCreatedAt(), t.getCreatedTimeOffset(), from, to))
                    .toList();
        }

        List<LedgerResponse> ledgerEntries = new ArrayList<>(
                mapTransactionsToLedger(transactions)
        );

        // Merge all credit limit history
        List<CreditLimitHistory> allCreditHistory = creditLimitHistoryRepository.findAll(Sort.by("createdAt").descending());

        if (from != null || to != null) {
            allCreditHistory = allCreditHistory.stream()
                    .filter(h -> timestampMapper.isInUserDateRange(
                            h.getCreatedAt(), h.getCreatedTimeOffset(), from, to))
                    .toList();
        }

        if (transactionStatus != null) {
            allCreditHistory = allCreditHistory.stream()
                    .filter(h -> matchesCreditLimitStatus(h, transactionStatus))
                    .toList();
        }

        allCreditHistory.stream()
                .map(this::mapCreditLimitToLedgerResponse)
                .forEach(ledgerEntries::add);

        // Sort merged list by transactionId ASC (nulls last for credit-limit entries)
        ledgerEntries.sort(Comparator.comparing(LedgerResponse::getTransactionId,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return ledgerEntries;
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private Long getUserBusinessId(Long userId) {
        return userRepository.findById(userId)
                .map(User::getBusiness)
                .map(BusinessEntity::getId)
                .orElse(null);
    }

    /** Maps a CreditLimitHistory record to a LedgerResponse (no transaction involved). */
    private LedgerResponse mapCreditLimitToLedgerResponse(CreditLimitHistory history) {
        String businessName = businessRepository.findById(history.getBusinessId())
                .map(BusinessEntity::getCompanyName)
                .orElse(null);

        String createdByName = null;
        if (history.getCreatedBy() != null) {
            createdByName = adminUserRepository.findById(history.getCreatedBy())
                    .map(AdminUser::getFullName)
                    .orElse(null);
        }

        TransactionStatus txStatus = (history.getStatus() == CreditLimitStatus.DEBIT)
                ? TransactionStatus.DEBIT
                : TransactionStatus.CREDIT;

        String type = "CREDIT_LIMIT_" + history.getStatus().name();

        LedgerResponse.CreditLimitInfo creditLimitInfo = LedgerResponse.CreditLimitInfo.builder()
                .creditLimitHistoryId(history.getId())
                .businessId(history.getBusinessId())
                .businessName(businessName)
                .amount(history.getAmount())
                .cause(history.getCause())
                .returnDate(history.getReturnDate())
                .adminInstruction(history.getAdminInstruction())
                .creditLimitStatus(history.getStatus())
                .balanceBefore(history.getBalanceBefore())
                .balanceAfter(history.getBalanceAfter())
                .createdByName(createdByName)
                .build();

        return LedgerResponse.builder()
                .type(type)
                .amount(history.getAmount() != null ? history.getAmount().doubleValue() : null)
                .description(history.getCause())
                .createdAt(timestampMapper.toRequestUserTime(history.getCreatedAt(), history.getCreatedTimeOffset()))
                .transactionStatus(txStatus)
                .creditLimitInfo(creditLimitInfo)
                .build();
    }

    /** Returns true if a credit limit history entry matches the requested TransactionStatus filter. */
    private boolean matchesCreditLimitStatus(CreditLimitHistory history, TransactionStatus status) {
        if (status == TransactionStatus.CREDIT) {
            return history.getStatus() == CreditLimitStatus.CREDIT
                    || history.getStatus() == CreditLimitStatus.REFUND;
        } else if (status == TransactionStatus.DEBIT) {
            return history.getStatus() == CreditLimitStatus.DEBIT;
        }
        return true;
    }

    // ─── original helpers (unchanged) ───────────────────────────────────────────

    private List<String> getTransactionTypesByStatus(TransactionStatus status) {
        if (status == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(DepositType.values())
                .filter(type -> DepositTypeUtil.determineStatus(type.name()) == status)
                .map(DepositType::name)
                .toList();
    }

    private List<LedgerResponse> mapTransactionsToLedger(List<Transaction> transactions) {
        Map<String, Map<Long, ?>> caches = transactionEnrichmentService.buildCaches(transactions);
        return transactions.stream()
                .map(txn -> mapToLedgerResponse(txn, caches))
                .toList();
    }

    private LedgerResponse mapToLedgerResponse(Transaction txn, Map<String, Map<Long, ?>> caches) {

        TransactionStatus status = DepositTypeUtil.determineStatus(txn.getType());
        Optional<User> user = userRepository.findById(txn.getUserId());
        String agentCode = null;
        String agentName = null;
        if (user.isPresent()) {
            User userDetails = user.get();
            if (userDetails.getBusiness() != null || userDetails.getChildrenUsers() != null) {
                agentCode = userDetails.getCode();
                agentName = userDetails.getFullName();
            }
        }

        SourceEnrichment enrichment = transactionEnrichmentService.enrich(txn, caches);

        LedgerResponse.LedgerResponseBuilder builder = LedgerResponse.builder()
                .transactionId(txn.getId())
                .type(txn.getType())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .exchangeRate(txn.getExchangeRate())
                .description(txn.getDescription())
                .convertedAmount(
                        txn.getConvertedAmount() != null
                                ? Double.valueOf(txn.getConvertedAmount())
                                : null
                ).transactionStatus(status)
                .createdAt(timestampMapper.createdAt(txn))
                .createdBy(txn.getCreatedBy())
                .agentCode(agentCode)
                .agentName(agentName)
                .reference(txn.getReference())
                .sourceType(txn.getSourceType())
                .sourceId(txn.getSourceId());

        if (enrichment.getLabel() != null || enrichment.getDetail() != null || enrichment.getStatus() != null) {
            builder.sourceSummary(LedgerResponse.SourceSummary.builder()
                    .label(enrichment.getLabel())
                    .detail(enrichment.getDetail())
                    .status(enrichment.getStatus())
                    .build());
        }

        if (isDepositType(txn.getType())) {
            LedgerResponse.DepositInfo depositInfo = enrichment.getDepositInfo();
            if (depositInfo == null) {
                depositInfo = depositTransactionSourceResolver.resolveDeposit(txn, Map.of())
                        .map(depositTransactionSourceResolver::mapDepositInfo)
                        .orElse(null);
            }
            if (depositInfo != null) {
                builder.depositInfo(depositInfo);
            }
        }

        if (isDeductionType(txn.getType()) && enrichment.getBookingInfo() != null) {
            builder.bookingInfo(enrichment.getBookingInfo());
        }

        return builder.build();
    }

    private boolean isDepositType(String type) {
        if (type == null) return false;
        return type.equalsIgnoreCase("DEPOSIT") ||
                type.equalsIgnoreCase("REFUND") ||
                type.equalsIgnoreCase("CREDIT");
    }

    private boolean isDeductionType(String type) {
        if (type == null) return false;
        return type.equalsIgnoreCase("BOOKING_DEDUCTION") ||
                type.equalsIgnoreCase("DEDUCTION") ||
                type.equalsIgnoreCase("WITHDRAWAL") ||
                type.equalsIgnoreCase("PURCHASE");
    }
}

