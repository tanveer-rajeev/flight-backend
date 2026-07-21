package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.BalanceChangeHistory;
import com.aerionsoft.application.entity.wallet.DepositBank;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.payment.StripeCredRepository;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.wallet.BalanceChangeHistoryRepository;
import com.aerionsoft.application.repository.wallet.CreditLimitHistoryRepository;
import com.aerionsoft.application.repository.wallet.DepositBankRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositListDao;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.audit.ActivityAdminAuditSupport;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.service.notification.NotificationHelper;
import com.aerionsoft.application.util.TimestampMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Deposit cancel / reverse behaviour from docs/deposit-flow-and-delete.md:
 * PENDING → reject (no balance change);
 * APPROVED → deleteTransaction (hard-delete deposit + txn, reverse balance + history);
 * bank ledger is NOT reversed by deleteTransaction.
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceDepositDeleteTest {

    private static final Long USER_ID = 10L;
    private static final Long ADMIN_ID = 99L;
    private static final Long DEPOSIT_ID = 100L;
    private static final Long TXN_ID = 200L;
    private static final Double AMOUNT = 500.0;
    private static final String REFERENCE = "dpTEST001";

    @Mock private WalletDepositRepository depositRepo;
    @Mock private WalletDepositListDao walletDepositListDao;
    @Mock private DepositBankRepository bankRepo;
    @Mock private BankLedgerService bankLedgerService;
    @Mock private UserRepository userRepo;
    @Mock private StripeCredRepository stripeCredRepository;
    @Mock private TransactionRepository transactionRepo;
    @Mock private TransactionService transactionService;
    @Mock private ReferenceGeneratorService referenceGeneratorService;
    @Mock private CurrencyService currencyService;
    @Mock private BusinessService businessService;
    @Mock private NotificationHelper notificationHelper;
    @Mock private AdminUserRepository adminUserRepository;
    @Mock private CreditLimitValidatorService creditLimitValidatorService;
    @Mock private RoleAssignmentRepository roleAssignmentRepository;
    @Mock private BalanceChangeHistoryRepository balanceChangeHistoryRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private CreditLimitHistoryRepository creditLimitHistoryRepository;
    @Mock private CreditLimitService creditLimitService;
    @Mock private ActivityAdminAuditSupport activityAdminAuditSupport;

    @Spy
    private TimestampMapper timestampMapper = new TimestampMapper();

    @InjectMocks
    private WalletService walletService;

    private User user;
    private AdminUser admin;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email("agent@example.com")
                .password("x")
                .fullName("Test Agent")
                .balance(1000.0)
                .currency("USD")
                .build();

        admin = new AdminUser();
        admin.setId(ADMIN_ID);
        admin.setFullName("Admin User");
    }

    // -------------------------------------------------------------------------
    // Step 1 — status identification side effects on approve
    // -------------------------------------------------------------------------

    @Test
    void approvePendingCashDeposit_creditsBalanceAndWritesLedgerTables_butBankOnlyWhenLinked() {
        WalletDeposit deposit = pendingDeposit(DepositType.CASH, null);

        when(depositRepo.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
        when(depositRepo.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepo.updateBalance(eq(USER_ID), anyDouble())).thenReturn(1);
        when(userRepo.findByIdInWithParent(anyCollection())).thenReturn(List.of(user));
        when(adminUserRepository.findAllById(anyCollection())).thenReturn(List.of(admin));
        when(businessRepository.findCompanyNamesByMotherUserIds(anyCollection())).thenReturn(List.of());

        var response = walletService.approveOrReject(DEPOSIT_ID, DepositStatus.APPROVED, ADMIN_ID, "ok");

        assertThat(response.getStatus()).isEqualTo(DepositStatus.APPROVED);
        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.APPROVED);
        assertThat(deposit.getApprovedBy()).isEqualTo(ADMIN_ID);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepo).saveAndFlush(txnCaptor.capture());
        Transaction savedTxn = txnCaptor.getValue();
        assertThat(savedTxn.getSourceType()).isEqualTo(TransactionSourceType.DEPOSIT.name());
        assertThat(savedTxn.getSourceId()).isEqualTo(DEPOSIT_ID);
        assertThat(savedTxn.getAmount()).isEqualTo(AMOUNT);
        assertThat(savedTxn.getReference()).isEqualTo(REFERENCE);

        verify(userRepo).updateBalance(USER_ID, 1500.0);

        ArgumentCaptor<BalanceChangeHistory> histCaptor = ArgumentCaptor.forClass(BalanceChangeHistory.class);
        verify(balanceChangeHistoryRepository).save(histCaptor.capture());
        BalanceChangeHistory hist = histCaptor.getValue();
        assertThat(hist.getChangeType()).isEqualTo("CREDIT");
        assertThat(hist.getReferenceType()).isEqualTo("DEPOSIT");
        assertThat(hist.getReferenceId()).isEqualTo(DEPOSIT_ID);
        assertThat(hist.getAmount()).isEqualTo(AMOUNT);

        verify(bankLedgerService).recordAgentDepositApproval(deposit, ADMIN_ID);
        verify(activityAdminAuditSupport).logDepositDecision(
                eq(DEPOSIT_ID), eq(USER_ID), eq(DepositStatus.APPROVED), eq(AMOUNT), eq("ok"));
    }

    @Test
    void approveBankLinkedDeposit_callsBankLedgerWithDepositBank() {
        DepositBank bank = bank(7L, new BigDecimal("2000.00"));
        WalletDeposit deposit = pendingDeposit(DepositType.BANK_TRANSFER_OR_MFS, bank);

        when(depositRepo.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
        when(depositRepo.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepo.updateBalance(eq(USER_ID), anyDouble())).thenReturn(1);
        when(userRepo.findByIdInWithParent(anyCollection())).thenReturn(List.of(user));
        when(adminUserRepository.findAllById(anyCollection())).thenReturn(List.of(admin));
        when(businessRepository.findCompanyNamesByMotherUserIds(anyCollection())).thenReturn(List.of());

        walletService.approveOrReject(DEPOSIT_ID, DepositStatus.APPROVED, ADMIN_ID, "bank ok");

        verify(bankLedgerService).recordAgentDepositApproval(
                argThat(d -> d.getDepositBank() != null && d.getDepositBank().getId().equals(7L)),
                eq(ADMIN_ID));
    }

    // -------------------------------------------------------------------------
    // Step 2 — PENDING reject (no money moved)
    // -------------------------------------------------------------------------

    @Test
    void rejectPendingDeposit_updatesStatusOnly_noBalanceOrTransaction() {
        WalletDeposit deposit = pendingDeposit(DepositType.CASH, null);

        when(depositRepo.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
        when(depositRepo.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findByIdInWithParent(anyCollection())).thenReturn(List.of(user));
        when(adminUserRepository.findAllById(anyCollection())).thenReturn(List.of(admin));
        when(businessRepository.findCompanyNamesByMotherUserIds(anyCollection())).thenReturn(List.of());

        var response = walletService.approveOrReject(DEPOSIT_ID, DepositStatus.REJECTED, ADMIN_ID, "duplicate");

        assertThat(response.getStatus()).isEqualTo(DepositStatus.REJECTED);
        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.REJECTED);
        assertThat(deposit.getRemarks()).isEqualTo("duplicate");

        verify(transactionRepo, never()).saveAndFlush(any());
        verify(userRepo, never()).updateBalance(anyLong(), anyDouble());
        verify(balanceChangeHistoryRepository, never()).save(any());
        verify(bankLedgerService, never()).recordAgentDepositApproval(any(), any());
        verify(activityAdminAuditSupport).logDepositDecision(
                eq(DEPOSIT_ID), eq(USER_ID), eq(DepositStatus.REJECTED), eq(AMOUNT), eq("duplicate"));
    }

    @Test
    void approveOrReject_nonPendingDeposit_throws() {
        WalletDeposit deposit = pendingDeposit(DepositType.CASH, null);
        deposit.setStatus(DepositStatus.APPROVED);

        when(depositRepo.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));

        assertThatThrownBy(() ->
                walletService.approveOrReject(DEPOSIT_ID, DepositStatus.REJECTED, ADMIN_ID, "too late"))
                .hasMessageContaining("pending");

        verify(depositRepo, never()).save(any());
        verify(transactionRepo, never()).saveAndFlush(any());
        verify(userRepo, never()).updateBalance(anyLong(), anyDouble());
    }

    // -------------------------------------------------------------------------
    // Step 3 — APPROVED reverse via deleteTransaction
    // -------------------------------------------------------------------------

    @Test
    void deleteTransaction_forApprovedDeposit_removesDepositAndTxn_andReversesBalanceHistory() {
        WalletDeposit deposit = approvedDeposit(DepositType.CASH, null);
        Transaction txn = depositTransaction(deposit);

        BalanceChangeHistory creditRow = BalanceChangeHistory.builder()
                .id(55L)
                .userId(USER_ID)
                .changeType("CREDIT")
                .amount(AMOUNT)
                .balanceBefore(1000.0)
                .balanceAfter(1500.0)
                .referenceType("DEPOSIT")
                .referenceId(DEPOSIT_ID)
                .build();

        when(transactionRepo.findById(TXN_ID)).thenReturn(Optional.of(txn));
        when(depositRepo.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepo.updateBalance(USER_ID, 500.0)).thenReturn(1);
        when(balanceChangeHistoryRepository.findByUserIdAndReferenceTypeAndReferenceIdAndChangeType(
                USER_ID, "DEPOSIT", DEPOSIT_ID, "CREDIT"))
                .thenReturn(List.of(creditRow));

        walletService.deleteTransaction(TXN_ID);

        verify(depositRepo).delete(deposit);
        verify(depositRepo).flush();
        verify(transactionRepo).delete(txn);
        verify(transactionRepo).flush();
        verify(userRepo).updateBalance(USER_ID, 500.0); // 1000 - 500 credit reversed
        verify(balanceChangeHistoryRepository).deleteAllById(List.of(55L));
    }

    @Test
    void deleteTransaction_findsDepositByReferenceWhenSourceIdMissing() {
        WalletDeposit deposit = approvedDeposit(DepositType.BANK_DEPOSIT, null);
        Transaction txn = Transaction.builder()
                .id(TXN_ID)
                .userId(USER_ID)
                .type(DepositType.BANK_DEPOSIT.name())
                .amount(AMOUNT)
                .sourceType(TransactionSourceType.BOOKING.name()) // not DEPOSIT path
                .sourceId(null)
                .reference(REFERENCE)
                .active(true)
                .build();

        when(transactionRepo.findById(TXN_ID)).thenReturn(Optional.of(txn));
        when(depositRepo.findByReference(REFERENCE)).thenReturn(Optional.of(deposit));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepo.updateBalance(eq(USER_ID), anyDouble())).thenReturn(1);

        walletService.deleteTransaction(TXN_ID);

        verify(depositRepo).findByReference(REFERENCE);
        verify(depositRepo).delete(deposit);
        verify(transactionRepo).delete(txn);
    }

    // -------------------------------------------------------------------------
    // Step 4 — bank ledger gap: deleteTransaction does not reverse bank books
    // -------------------------------------------------------------------------

    @Test
    void deleteTransaction_doesNotReverseBankLedgerOrDepositBankBalance() {
        DepositBank bank = bank(7L, new BigDecimal("2500.00"));
        WalletDeposit deposit = approvedDeposit(DepositType.BANK_TRANSFER_OR_MFS, bank);
        Transaction txn = depositTransaction(deposit);

        when(transactionRepo.findById(TXN_ID)).thenReturn(Optional.of(txn));
        when(depositRepo.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepo.updateBalance(eq(USER_ID), anyDouble())).thenReturn(1);
        when(balanceChangeHistoryRepository.findByUserIdAndReferenceTypeAndReferenceIdAndChangeType(
                USER_ID, "DEPOSIT", DEPOSIT_ID, "CREDIT"))
                .thenReturn(List.of());

        walletService.deleteTransaction(TXN_ID);

        // Gap documented in deposit-flow-and-delete.md: bank side must be fixed manually
        verifyNoInteractions(bankLedgerService);
        verifyNoInteractions(bankRepo);
        assertThat(bank.getCurrentBalance()).isEqualByComparingTo("2500.00");
    }

    /**
     * Documents the manual bank reverse steps expected after deleteTransaction
     * when a WALLET_DEPOSIT bank ledger CREDIT was written on approval.
     */
    @Test
    void manualBankLedgerReverse_afterApprovedDepositDelete_restoresBankBalance() {
        DepositBank bank = bank(7L, new BigDecimal("2500.00"));
        BigDecimal depositAmount = BigDecimal.valueOf(AMOUNT);

        // Simulate post-deleteTransaction manual fix from the runbook
        bank.setCurrentBalance(bank.getCurrentBalance().subtract(depositAmount));

        assertThat(bank.getCurrentBalance()).isEqualByComparingTo("2000.00");
    }

    // -------------------------------------------------------------------------
    // Step 5 — consistency after reverse
    // -------------------------------------------------------------------------

    @Test
    void deleteTransaction_leavesNoDepositCreditHistoryForThatDeposit() {
        WalletDeposit deposit = approvedDeposit(DepositType.CASH, null);
        Transaction txn = depositTransaction(deposit);
        BalanceChangeHistory creditRow = BalanceChangeHistory.builder()
                .id(77L)
                .userId(USER_ID)
                .changeType("CREDIT")
                .amount(AMOUNT)
                .balanceBefore(1000.0)
                .balanceAfter(1500.0)
                .referenceType("DEPOSIT")
                .referenceId(DEPOSIT_ID)
                .build();

        when(transactionRepo.findById(TXN_ID)).thenReturn(Optional.of(txn));
        when(depositRepo.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepo.updateBalance(USER_ID, 500.0)).thenReturn(1);
        when(balanceChangeHistoryRepository.findByUserIdAndReferenceTypeAndReferenceIdAndChangeType(
                USER_ID, "DEPOSIT", DEPOSIT_ID, "CREDIT"))
                .thenReturn(List.of(creditRow));

        walletService.deleteTransaction(TXN_ID);

        ArgumentCaptor<Iterable<Long>> idsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(balanceChangeHistoryRepository).deleteAllById(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(77L);

        verify(depositRepo).delete(deposit);
        verify(transactionRepo).delete(txn);
        verify(userRepo).updateBalance(USER_ID, 500.0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WalletDeposit pendingDeposit(DepositType type, DepositBank bank) {
        return WalletDeposit.builder()
                .id(DEPOSIT_ID)
                .userId(USER_ID)
                .type(type)
                .status(DepositStatus.PENDING)
                .amount(AMOUNT)
                .currency(Currency.USD)
                .reference(REFERENCE)
                .transactionId("txn-" + DEPOSIT_ID)
                .depositBank(bank)
                .build();
    }

    private WalletDeposit approvedDeposit(DepositType type, DepositBank bank) {
        WalletDeposit d = pendingDeposit(type, bank);
        d.setStatus(DepositStatus.APPROVED);
        d.setApprovedBy(ADMIN_ID);
        return d;
    }

    private Transaction depositTransaction(WalletDeposit deposit) {
        return Transaction.builder()
                .id(TXN_ID)
                .userId(USER_ID)
                .type(deposit.getType().name())
                .amount(AMOUNT)
                .sourceType(TransactionSourceType.DEPOSIT.name())
                .sourceId(deposit.getId())
                .reference(deposit.getReference())
                .active(true)
                .build();
    }

    private DepositBank bank(Long id, BigDecimal currentBalance) {
        DepositBank bank = new DepositBank();
        bank.setId(id);
        bank.setBankName("Test Bank");
        bank.setCurrency("USD");
        bank.setIsActive(true);
        bank.setOpeningBalance(BigDecimal.ZERO);
        bank.setCurrentBalance(currentBalance);
        return bank;
    }
}
