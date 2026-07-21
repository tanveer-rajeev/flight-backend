package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.admin.bank.BankLedgerEntryResponse;
import com.aerionsoft.application.entity.wallet.BankLedgerEntry;
import com.aerionsoft.application.entity.wallet.DepositBank;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.BankLedgerEntryType;
import com.aerionsoft.application.enums.wallet.BankLedgerSourceType;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.repository.wallet.BankLedgerEntryRepository;
import com.aerionsoft.application.repository.wallet.DepositBankRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Bank-side effects when a bank-linked deposit is approved, and the manual
 * reverse expected after wallet deleteTransaction (which does not touch bank books).
 */
@ExtendWith(MockitoExtension.class)
class BankLedgerServiceDepositApprovalTest {

    @Mock
    private BankLedgerEntryRepository bankLedgerEntryRepository;
    @Mock
    private DepositBankRepository depositBankRepository;

    @InjectMocks
    private BankLedgerService bankLedgerService;

    @Test
    void recordAgentDepositApproval_bankLinkedType_creditsBankAndWritesWalletDepositSource() {
        DepositBank bank = bank(7L, new BigDecimal("2000.00"));
        WalletDeposit deposit = deposit(DepositType.BANK_TRANSFER_OR_MFS, bank);

        when(bankLedgerEntryRepository.existsBySourceTypeAndSourceId(
                BankLedgerSourceType.WALLET_DEPOSIT, 100L)).thenReturn(false);
        when(depositBankRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(bank));
        when(bankLedgerEntryRepository.save(any(BankLedgerEntry.class))).thenAnswer(inv -> {
            BankLedgerEntry e = inv.getArgument(0);
            e.setId(50L);
            return e;
        });
        when(depositBankRepository.save(any(DepositBank.class))).thenAnswer(inv -> inv.getArgument(0));

        BankLedgerEntryResponse response = bankLedgerService.recordAgentDepositApproval(deposit, 99L);

        assertThat(response).isNotNull();
        assertThat(response.getSourceType()).isEqualTo(BankLedgerSourceType.WALLET_DEPOSIT);
        assertThat(response.getSourceId()).isEqualTo(100L);
        assertThat(response.getEntryType()).isEqualTo(BankLedgerEntryType.CREDIT);
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");

        ArgumentCaptor<BankLedgerEntry> entryCaptor = ArgumentCaptor.forClass(BankLedgerEntry.class);
        verify(bankLedgerEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getBalanceAfter()).isEqualByComparingTo("2500.00");

        assertThat(bank.getCurrentBalance()).isEqualByComparingTo("2500.00");
        verify(depositBankRepository).save(bank);
    }

    @Test
    void recordAgentDepositApproval_cashWithoutBank_skipsBankLedger() {
        WalletDeposit deposit = deposit(DepositType.CASH, null);

        BankLedgerEntryResponse response = bankLedgerService.recordAgentDepositApproval(deposit, 99L);

        assertThat(response).isNull();
        verifyNoInteractions(bankLedgerEntryRepository, depositBankRepository);
    }

    @Test
    void recordAgentDepositApproval_idempotentWhenEntryAlreadyExists() {
        DepositBank bank = bank(7L, new BigDecimal("2500.00"));
        WalletDeposit deposit = deposit(DepositType.BANK_DEPOSIT, bank);
        BankLedgerEntry existing = BankLedgerEntry.builder()
                .id(50L)
                .bank(bank)
                .entryType(BankLedgerEntryType.CREDIT)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .balanceAfter(new BigDecimal("2500.00"))
                .sourceType(BankLedgerSourceType.WALLET_DEPOSIT)
                .sourceId(100L)
                .build();

        when(bankLedgerEntryRepository.existsBySourceTypeAndSourceId(
                BankLedgerSourceType.WALLET_DEPOSIT, 100L)).thenReturn(true);
        when(bankLedgerEntryRepository.findBySourceTypeAndSourceId(
                BankLedgerSourceType.WALLET_DEPOSIT, 100L)).thenReturn(Optional.of(existing));

        BankLedgerEntryResponse response = bankLedgerService.recordAgentDepositApproval(deposit, 99L);

        assertThat(response.getId()).isEqualTo(50L);
        verify(depositBankRepository, never()).findByIdForUpdate(anyLong());
        verify(bankLedgerEntryRepository, never()).save(any());
    }

    /**
     * Manual reverse after deleteTransaction (runbook Step 4):
     * delete WALLET_DEPOSIT ledger row and subtract CREDIT amount from deposit_bank.
     */
    @Test
    void manualBankReverse_afterWalletDepositDelete_removesEntryAndRestoresBankBalance() {
        DepositBank bank = bank(7L, new BigDecimal("2500.00"));
        BankLedgerEntry entry = BankLedgerEntry.builder()
                .id(50L)
                .bank(bank)
                .entryType(BankLedgerEntryType.CREDIT)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .balanceAfter(new BigDecimal("2500.00"))
                .sourceType(BankLedgerSourceType.WALLET_DEPOSIT)
                .sourceId(100L)
                .build();

        when(bankLedgerEntryRepository.findBySourceTypeAndSourceId(
                BankLedgerSourceType.WALLET_DEPOSIT, 100L)).thenReturn(Optional.of(entry));

        BankLedgerEntry found = bankLedgerEntryRepository
                .findBySourceTypeAndSourceId(BankLedgerSourceType.WALLET_DEPOSIT, 100L)
                .orElseThrow();

        bank.setCurrentBalance(bank.getCurrentBalance().subtract(found.getAmount()));
        bankLedgerEntryRepository.delete(found);
        depositBankRepository.save(bank);

        assertThat(bank.getCurrentBalance()).isEqualByComparingTo("2000.00");
        verify(bankLedgerEntryRepository).delete(entry);
        verify(depositBankRepository).save(bank);
    }

    private WalletDeposit deposit(DepositType type, DepositBank bank) {
        return WalletDeposit.builder()
                .id(100L)
                .userId(10L)
                .type(type)
                .status(DepositStatus.APPROVED)
                .amount(500.0)
                .currency(Currency.USD)
                .reference("dpBANK001")
                .depositBank(bank)
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
