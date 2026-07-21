package com.aerionsoft.application.service.payment;

import com.aerionsoft.application.dto.payment.PaymentCompletionRequest;
import com.aerionsoft.application.dto.transaction.TransactionCreateDto;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.wallet.TransactionService;
import com.aerionsoft.application.service.wallet.WalletService;
import com.aerionsoft.application.util.UserDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentCompletionServiceImpl implements PaymentCompletionService {

    private final WalletDepositRepository walletDepositRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final WalletService walletService;

    @Override
    public void completeDeposit(PaymentCompletionRequest request) {

        log.info("Starting deposit completion: userId={}, depositType={}, amount={} {}, reference={}, paymentTransactionId={}",
                request.getUserId(),
                request.getDepositType(),
                request.getAmount(),
                request.getCurrency(),
                request.getReference(),
                request.getPaymentTransactionId());

        try {
            WalletDeposit deposit = WalletDeposit.builder()
                    .userId(request.getUserId())
                    .type(request.getDepositType())
                    .status(DepositStatus.APPROVED)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .reference(request.getReference())
                    .transactionId(request.getPaymentTransactionId())
                    .remarks(request.getDescription())
                    .createdAt(UserDateTimeUtil.now())
                    .approvedAt(UserDateTimeUtil.now())
                    .approvedBy(request.getCreatedBy())
                    .build();

            deposit = walletDepositRepository.save(deposit);
            log.debug("WalletDeposit persisted: depositId={}, status={}", deposit.getId(), deposit.getStatus());

            TransactionCreateDto transactionDto = new TransactionCreateDto();
            transactionDto.setAmount(request.getAmount());
            transactionDto.setConvertedAmount(String.valueOf(deposit.getAmount()));
            transactionDto.setCurrency(request.getCurrency().name());
            transactionDto.setDescription(request.getDescription());
            transactionDto.setType(request.getDepositType().name());
            transactionDto.setUserId(request.getUserId());

            Transaction transaction = transactionService.createTransaction(transactionDto, request.getCreatedBy());
            log.debug("Transaction created: transactionId={}, userId={}", transaction.getId(), request.getUserId());

            transaction.setSourceType(TransactionSourceType.DEPOSIT.name());
            transaction.setSourceId(deposit.getId());
            transaction.setReference(deposit.getReference());

            transactionRepository.save(transaction);
            log.debug("Transaction linked to deposit: transactionId={}, sourceId={}, reference={}",
                    transaction.getId(), deposit.getId(), deposit.getReference());

            walletService.addToWalletBalance(
                    request.getUserId(),
                    request.getAmount(),
                    request.getDepositType().name(),
                    request.getPaymentProvider(),
                    deposit.getId(),
                    TransactionSourceType.DEPOSIT.name(),
                    request.getCreatedBy());

            log.info("Deposit completion succeeded: userId={}, depositId={}, transactionId={}, amount={} {}",
                    request.getUserId(),
                    deposit.getId(),
                    transaction.getId(),
                    request.getAmount(),
                    request.getCurrency());

        } catch (Exception ex) {
            log.error("Deposit completion failed: userId={}, reference={}, paymentTransactionId={}, amount={} {}",
                    request.getUserId(),
                    request.getReference(),
                    request.getPaymentTransactionId(),
                    request.getAmount(),
                    request.getCurrency(),
                    ex);
            throw ex;
        }
    }
}