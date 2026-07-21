package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.ledger.LedgerResponse;
import com.aerionsoft.application.dto.wallet.SourceEnrichment;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DepositTransactionSourceResolver implements TransactionSourceResolver {

    private final WalletDepositRepository walletDepositRepository;

    public DepositTransactionSourceResolver(WalletDepositRepository walletDepositRepository) {
        this.walletDepositRepository = walletDepositRepository;
    }

    @Override
    public boolean supports(String sourceType) {
        return TransactionSourceType.DEPOSIT.name().equals(sourceType);
    }

    @Override
    public Map<Long, ?> batchLoad(Collection<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return walletDepositRepository.findAllById(sourceIds).stream()
                .collect(Collectors.toMap(WalletDeposit::getId, Function.identity()));
    }

    @Override
    public SourceEnrichment enrich(Transaction txn, Map<Long, ?> batchCache) {
        Optional<WalletDeposit> depositOpt = resolveDeposit(txn, batchCache);
        return depositOpt.map(deposit -> {
            LedgerResponse.DepositInfo depositInfo = mapDepositInfo(deposit);
            return SourceEnrichment.builder()
                    .sourceType(TransactionSourceType.DEPOSIT.name())
                    .sourceId(deposit.getId())
                    .label(deposit.getReference())
                    .detail(deposit.getType() != null ? deposit.getType().name() : null)
                    .status(deposit.getStatus() != null ? deposit.getStatus().name() : null)
                    .depositInfo(depositInfo)
                    .build();
        }).orElse(SourceEnrichment.empty());
    }

    public Optional<WalletDeposit> resolveDeposit(Transaction txn, Map<Long, ?> batchCache) {
        if (TransactionSourceType.DEPOSIT.name().equals(txn.getSourceType()) && txn.getSourceId() != null) {
            Object cached = batchCache != null ? batchCache.get(txn.getSourceId()) : null;
            if (cached instanceof WalletDeposit deposit) {
                return Optional.of(deposit);
            }
            return walletDepositRepository.findById(txn.getSourceId());
        }
        if (txn.getReference() != null) {
            return walletDepositRepository.findByReference(txn.getReference());
        }
        return Optional.empty();
    }

    public LedgerResponse.DepositInfo mapDepositInfo(WalletDeposit deposit) {
        return LedgerResponse.DepositInfo.builder()
                .depositId(deposit.getId())
                .depositType(deposit.getType() != null ? deposit.getType().name() : null)
                .depositStatus(deposit.getStatus() != null ? deposit.getStatus() : null)
                .depositReference(deposit.getReference())
                .remarks(deposit.getRemarks())
                .depositBank(deposit.getDepositBank() != null ? deposit.getDepositBank().getBankName() : null)
                .exchangeRate(deposit.getExchangeRate())
                .exchangedAmount(deposit.getExchangedAmount())
                .depositCurrency(deposit.getCurrency() != null ? deposit.getCurrency().name() : null)
                .approvedAt(deposit.getApprovedAt())
                .build();
    }
}
