package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.admin.bank.DepositBankRequest;
import com.aerionsoft.application.dto.admin.bank.DepositBankResponse;
import com.aerionsoft.application.entity.wallet.DepositBank;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.repository.wallet.DepositBankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepositBankService {

    @Autowired
    private DepositBankRepository depositBankRepository;

    public List<DepositBankResponse> getAll() {
        return depositBankRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DepositBankResponse create(DepositBankRequest request) {
        DepositBank bank = new DepositBank();
        bank.setBankName(request.getBankName());
        bank.setAccountName(request.getAccountName());
        bank.setAccountNumber(request.getAccountNumber());
        bank.setRoutingNumber(request.getRoutingNumber());
        bank.setBranch(request.getBranch());
        bank.setCurrency(normalizeCurrency(request.getCurrency()));
        bank.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        if (request.getOpeningBalance() != null) {
            bank.setOpeningBalance(request.getOpeningBalance());
            bank.setCurrentBalance(request.getOpeningBalance());
        }

        DepositBank saved = depositBankRepository.save(bank);
        return mapToResponse(saved);
    }

    public DepositBankResponse update(Long id, DepositBankRequest request) {
        DepositBank bank = depositBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit bank", id));

        bank.setBankName(request.getBankName());
        bank.setAccountName(request.getAccountName());
        bank.setAccountNumber(request.getAccountNumber());
        bank.setRoutingNumber(request.getRoutingNumber());
        bank.setBranch(request.getBranch());
        bank.setCurrency(normalizeCurrency(request.getCurrency()));
        if (request.getIsActive() != null) {
            bank.setIsActive(request.getIsActive());
        }
        if (request.getOpeningBalance() != null) {
            bank.setOpeningBalance(request.getOpeningBalance());
        }

        DepositBank updated = depositBankRepository.save(bank);
        return mapToResponse(updated);
    }

    public void delete(Long id) {
        depositBankRepository.deleteById(id);
    }

    private DepositBankResponse mapToResponse(DepositBank bank) {
        return DepositBankResponse.builder()
                .id(bank.getId())
                .bankName(bank.getBankName())
                .accountName(bank.getAccountName())
                .accountNumber(bank.getAccountNumber())
                .routingNumber(bank.getRoutingNumber())
                .branch(bank.getBranch())
                .currency(bank.getCurrency())
                .isActive(bank.getIsActive())
                .openingBalance(bank.getOpeningBalance())
                .currentBalance(bank.getCurrentBalance())
                .build();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase();
    }
}
