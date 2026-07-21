package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.dto.transaction.TransactionCreateDto;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(TransactionCreateDto transactionCreateDto, Long authUserId) {
        Transaction transaction = Transaction.builder()
                .amount(transactionCreateDto.getAmount())
                .convertedAmount(transactionCreateDto.getConvertedAmount())
                .currency(transactionCreateDto.getCurrency())
                .description(transactionCreateDto.getDescription())
                .type(transactionCreateDto.getType())
                .userId(transactionCreateDto.getUserId())
                .createdBy(String.valueOf(authUserId))
                .createdAt(UserDateTimeUtil.now())
                .active(true)
                .build();

        transaction = transactionRepository.save(transaction);
        return transaction;
    }
}
