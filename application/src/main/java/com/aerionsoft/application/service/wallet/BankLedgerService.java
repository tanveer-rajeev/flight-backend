package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.admin.bank.*;
import com.aerionsoft.application.enums.wallet.*;
import com.aerionsoft.application.entity.wallet.BankLedgerEntry;
import com.aerionsoft.application.entity.wallet.DepositBank;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.client.ManualInvoicePaymentType;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import com.aerionsoft.application.repository.wallet.BankLedgerEntryRepository;
import com.aerionsoft.application.repository.wallet.DepositBankRepository;
import com.aerionsoft.application.util.FilterRangeUtil;
import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.util.UserTimezoneUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class BankLedgerService {

    private static final Set<DepositType> BANK_LINKED_DEPOSIT_TYPES = EnumSet.of(
            DepositType.BANK_DEPOSIT,
            DepositType.BANK_TRANSFER_OR_MFS,
            DepositType.CHEQUE
    );

    @Autowired
    private BankLedgerEntryRepository bankLedgerEntryRepository;

    @Autowired
    private DepositBankRepository depositBankRepository;

    @Transactional(readOnly = true)
    public Page<BankLedgerEntryResponse> getLedger(
            Long bankId,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        DepositBank bank = depositBankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit bank", bankId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<BankLedgerEntry> spec = forBank(bank.getId())
                .and(createdInUserDateRange(from, to));

        Page<BankLedgerEntry> entries = bankLedgerEntryRepository.findAll(spec, pageable);

        return entries.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BankStatementResponse getStatement(Long bankId, LocalDate from, LocalDate to) {
        DepositBank bank = depositBankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit bank", bankId));

        LocalDate effectiveFrom = from != null ? from : UserDateTimeUtil.now().toLocalDate().withDayOfMonth(1);
        LocalDate effectiveTo = to != null ? to : UserDateTimeUtil.now().toLocalDate();

        BigDecimal openingBalance = computeOpeningBalance(bank, effectiveFrom);
        List<BankLedgerEntry> periodEntries = bankLedgerEntryRepository.findAll(
                forBank(bank.getId()).and(createdInUserDateRange(effectiveFrom, effectiveTo)),
                Sort.by(Sort.Direction.ASC, "createdAt"));

        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal running = openingBalance;
        List<BankLedgerEntryResponse> lines = new ArrayList<>();

        for (BankLedgerEntry entry : periodEntries) {
            if (entry.getEntryType() == BankLedgerEntryType.CREDIT) {
                totalCredits = totalCredits.add(entry.getAmount());
                running = running.add(entry.getAmount());
            } else {
                totalDebits = totalDebits.add(entry.getAmount());
                running = running.subtract(entry.getAmount());
            }
            lines.add(toResponse(entry));
        }

        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        List<BankLedgerEntry> todayEntries = bankLedgerEntryRepository.findAll(
                forBank(bank.getId()).and(createdInUserDateRange(today, today)),
                Sort.by(Sort.Direction.ASC, "createdAt"));

        BigDecimal todayDeposits = sumByType(todayEntries, BankLedgerEntryType.CREDIT);
        BigDecimal todayWithdrawals = sumByType(todayEntries, BankLedgerEntryType.DEBIT);

        return BankStatementResponse.builder()
                .bankId(bank.getId())
                .bankName(bank.getBankName())
                .currency(bank.getCurrency())
                .from(effectiveFrom)
                .to(effectiveTo)
                .openingBalance(openingBalance)
                .closingBalance(running)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .todayDeposits(todayDeposits)
                .todayWithdrawals(todayWithdrawals)
                .entries(lines)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BankTodaySummaryItemResponse> getTodaySummary() {
        List<DepositBank> banks = depositBankRepository.findByIsActiveTrue();
        LocalDate today = UserDateTimeUtil.now().toLocalDate();

        List<BankTodaySummaryItemResponse> result = new ArrayList<>();
        for (DepositBank bank : banks) {
            List<BankLedgerEntry> todayEntries = bankLedgerEntryRepository.findAll(
                    forBank(bank.getId()).and(createdInUserDateRange(today, today)),
                    Sort.by(Sort.Direction.ASC, "createdAt"));
            BigDecimal todayDeposits = sumByType(todayEntries, BankLedgerEntryType.CREDIT);
            BigDecimal todayWithdrawals = sumByType(todayEntries, BankLedgerEntryType.DEBIT);

            result.add(BankTodaySummaryItemResponse.builder()
                    .bankId(bank.getId())
                    .bankName(bank.getBankName())
                    .currency(bank.getCurrency())
                    .currentBalance(nullToZero(bank.getCurrentBalance()))
                    .todayDeposits(todayDeposits)
                    .todayWithdrawals(todayWithdrawals)
                    .todayNet(todayDeposits.subtract(todayWithdrawals))
                    .build());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public BankLedgerEntryResponse recordAgentDepositApproval(WalletDeposit deposit, Long adminId) {
        if (deposit.getDepositBank() == null || !BANK_LINKED_DEPOSIT_TYPES.contains(deposit.getType())) {
            return null;
        }
        if (bankLedgerEntryRepository.existsBySourceTypeAndSourceId(
                BankLedgerSourceType.WALLET_DEPOSIT, deposit.getId())) {
            return bankLedgerEntryRepository
                    .findBySourceTypeAndSourceId(BankLedgerSourceType.WALLET_DEPOSIT, deposit.getId())
                    .map(this::toResponse)
                    .orElse(null);
        }

        String currency = resolveDepositCurrency(deposit);
        return toResponse(recordEntry(
                deposit.getDepositBank().getId(),
                BankLedgerEntryType.CREDIT,
                BankLedgerCategory.AGENT_DEPOSIT,
                BankEntryChannel.ONLINE,
                toBigDecimal(deposit.getAmount()),
                currency,
                BankLedgerSourceType.WALLET_DEPOSIT,
                deposit.getId(),
                deposit.getReference(),
                "Agent deposit approved: " + deposit.getType().name(),
                adminId != null ? "ADMIN:" + adminId : "SYSTEM"
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public BankLedgerEntryResponse recordAdminDeposit(
            Long depositBankId,
            Long walletDepositId,
            BigDecimal amount,
            Currency currency,
            String reference,
            String remarks,
            Long adminId
    ) {
        if (depositBankId == null) {
            return null;
        }
        if (bankLedgerEntryRepository.existsBySourceTypeAndSourceId(
                BankLedgerSourceType.ADMIN_DEPOSIT, walletDepositId)) {
            return bankLedgerEntryRepository
                    .findBySourceTypeAndSourceId(BankLedgerSourceType.ADMIN_DEPOSIT, walletDepositId)
                    .map(this::toResponse)
                    .orElse(null);
        }

        String description = "Admin deposit to agent wallet";
        if (remarks != null && !remarks.isBlank()) {
            description += " – " + remarks;
        }

        return toResponse(recordEntry(
                depositBankId,
                BankLedgerEntryType.CREDIT,
                BankLedgerCategory.ADMIN_DEPOSIT,
                BankEntryChannel.ADMIN,
                amount,
                currency.name(),
                BankLedgerSourceType.ADMIN_DEPOSIT,
                walletDepositId,
                reference,
                description,
                "ADMIN:" + adminId
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public BankLedgerEntryResponse recordSupplierPayment(
            Long depositBankId,
            Long supplierTransactionId,
            BigDecimal amount,
            String bankCurrency,
            String description,
            Long adminId
    ) {
        return toResponse(recordEntry(
                depositBankId,
                BankLedgerEntryType.DEBIT,
                BankLedgerCategory.SUPPLIER_PAYMENT,
                BankEntryChannel.ADMIN,
                amount,
                bankCurrency,
                BankLedgerSourceType.SUPPLIER_PAYMENT,
                supplierTransactionId,
                null,
                description != null ? description : "Supplier payment via bank",
                adminId != null ? "ADMIN:" + adminId : "SYSTEM"
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public BankLedgerEntryResponse recordDeposit(Long bankId, BankDepositRequest request, Long adminId) {
        BankLedgerEntry entry = recordEntry(
                bankId,
                BankLedgerEntryType.CREDIT,
                BankLedgerCategory.BANK_DEPOSIT,
                BankEntryChannel.ADMIN,
                request.getAmount(),
                null,
                BankLedgerSourceType.BANK_DEPOSIT,
                null,
                request.getReference(),
                request.getDescription(),
                "ADMIN:" + adminId
        );
        entry.setSourceId(entry.getId());
        bankLedgerEntryRepository.save(entry);
        return toResponse(entry);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankLedgerEntryResponse recordWithdrawal(Long bankId, BankWithdrawRequest request, Long adminId) {
        BankLedgerEntry entry = recordEntry(
                bankId,
                BankLedgerEntryType.DEBIT,
                BankLedgerCategory.BANK_WITHDRAWAL,
                BankEntryChannel.ADMIN,
                request.getAmount(),
                null,
                BankLedgerSourceType.BANK_WITHDRAWAL,
                null,
                request.getReference(),
                request.getDescription(),
                "ADMIN:" + adminId
        );
        entry.setSourceId(entry.getId());
        bankLedgerEntryRepository.save(entry);
        return toResponse(entry);
    }

    public void validateSupplierBankPayment(ManualInvoicePaymentType paymentType, Long depositBankId) {
        if (paymentType == ManualInvoicePaymentType.BANK && depositBankId == null) {
            throw ServiceExceptions.validation("depositBankId is required when payment type is BANK");
        }
    }

    public DepositBank resolveActiveBank(Long depositBankId) {
        DepositBank bank = depositBankRepository.findById(depositBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit bank", depositBankId));
        if (Boolean.FALSE.equals(bank.getIsActive())) {
            throw ServiceExceptions.invalidState("Deposit bank is inactive");
        }
        return bank;
    }

    public void validateCurrencyMatch(DepositBank bank, String currency) {
        if (currency == null || bank.getCurrency() == null) {
            return;
        }
        if (!bank.getCurrency().equalsIgnoreCase(currency.trim())) {
            throw ServiceExceptions.validation(
                    "Currency mismatch: bank is " + bank.getCurrency() + " but entry is " + currency);
        }
    }

    private BankLedgerEntry recordEntry(
            Long bankId,
            BankLedgerEntryType entryType,
            BankLedgerCategory category,
            BankEntryChannel channel,
            BigDecimal amount,
            String currency,
            BankLedgerSourceType sourceType,
            Long sourceId,
            String reference,
            String description,
            String createdBy
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ServiceExceptions.validation("Amount must be greater than zero");
        }

        DepositBank bank = depositBankRepository.findByIdForUpdate(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit bank", bankId));

        String entryCurrency = currency != null ? currency.trim().toUpperCase() : bank.getCurrency();
        validateCurrencyMatch(bank, entryCurrency);

        if (sourceId != null && bankLedgerEntryRepository.existsBySourceTypeAndSourceId(sourceType, sourceId)) {
            return bankLedgerEntryRepository.findBySourceTypeAndSourceId(sourceType, sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bank ledger entry"));
        }

        BigDecimal current = nullToZero(bank.getCurrentBalance());
        BigDecimal newBalance = entryType == BankLedgerEntryType.CREDIT
                ? current.add(amount)
                : current.subtract(amount);

        if (entryType == BankLedgerEntryType.DEBIT && newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw ServiceExceptions.insufficientBalance("Insufficient bank balance");
        }

        BankLedgerEntry entry = BankLedgerEntry.builder()
                .bank(bank)
                .entryType(entryType)
                .category(category)
                .entryChannel(channel)
                .amount(amount)
                .currency(entryCurrency)
                .balanceAfter(newBalance)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .reference(reference)
                .description(description)
                .createdAt(UserDateTimeUtil.now())
                .createdBy(createdBy)
                .build();

        entry = bankLedgerEntryRepository.save(entry);
        bank.setCurrentBalance(newBalance);
        depositBankRepository.save(bank);
        return entry;
    }

    private BigDecimal computeOpeningBalance(DepositBank bank, LocalDate fromDate) {
        BigDecimal base = nullToZero(bank.getOpeningBalance());
        Instant beforeStart = FilterRangeUtil.userDateRange(fromDate, null).start();
        Specification<BankLedgerEntry> spec = forBank(bank.getId());
        if (beforeStart != null) {
            Specification<BankLedgerEntry> beforeSpec = OffsetAwareDateSpec.wallClockInInstantRange(
                    null, beforeStart, "createdAt", UserTimezoneUtil.DEFAULT_OFFSET);
            if (beforeSpec != null) {
                spec = spec.and(beforeSpec);
            }
        }
        List<BankLedgerEntry> priorEntries = bankLedgerEntryRepository.findAll(
                spec, Sort.by(Sort.Direction.ASC, "createdAt"));
        for (BankLedgerEntry entry : priorEntries) {
            if (entry.getEntryType() == BankLedgerEntryType.CREDIT) {
                base = base.add(entry.getAmount());
            } else {
                base = base.subtract(entry.getAmount());
            }
        }
        return base;
    }

    private Specification<BankLedgerEntry> forBank(Long bankId) {
        return (root, query, cb) -> cb.equal(root.get("bank").get("id"), bankId);
    }

    private Specification<BankLedgerEntry> createdInUserDateRange(LocalDate from, LocalDate to) {
        Specification<BankLedgerEntry> spec = OffsetAwareDateSpec.wallClockInUserDateRange(
                from, to, "createdAt", UserTimezoneUtil.DEFAULT_OFFSET);
        return spec != null ? spec : (root, query, cb) -> cb.conjunction();
    }

    private BigDecimal sumByType(List<BankLedgerEntry> entries, BankLedgerEntryType type) {
        return entries.stream()
                .filter(e -> e.getEntryType() == type)
                .map(BankLedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BankLedgerEntryResponse toResponse(BankLedgerEntry entry) {
        return BankLedgerEntryResponse.builder()
                .id(entry.getId())
                .date(entry.getCreatedAt())
                .entryType(entry.getEntryType())
                .category(entry.getCategory())
                .entryChannel(entry.getEntryChannel())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .balanceAfter(entry.getBalanceAfter())
                .sourceType(entry.getSourceType())
                .sourceId(entry.getSourceId())
                .reference(entry.getReference())
                .description(entry.getDescription())
                .createdBy(entry.getCreatedBy())
                .build();
    }

    private String resolveDepositCurrency(WalletDeposit deposit) {
        if (deposit.getCurrency() != null) {
            return deposit.getCurrency().name();
        }
        return "USD";
    }

    private BigDecimal toBigDecimal(Double amount) {
        return amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public DepositBankSummaryResponse toSummary(DepositBank bank) {
        if (bank == null) {
            return null;
        }
        return DepositBankSummaryResponse.builder()
                .id(bank.getId())
                .bankName(bank.getBankName())
                .accountNumber(bank.getAccountNumber())
                .currency(bank.getCurrency())
                .build();
    }
}
