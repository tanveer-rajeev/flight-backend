package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.client.user.TransactionDto;
import com.aerionsoft.application.dto.report.AgencyDueItemDTO;
import com.aerionsoft.application.dto.report.AgencyDueReportDTO;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.util.TimestampMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgencyDueReportServiceImpl implements AgencyDueReportService {

    /** Transaction types that reduce the user's balance (cause or deepen negative balance). */
    private static final List<String> DEBIT_TYPES = List.of(
            "PURCHASE",
            "BOOKING_DEDUCTION",
            "BOOKING_STATUS_UPDATE_DEDUCTION",
            "ADMIN_CHARGE"
    );

    private final BusinessRepository businessRepository;
    private final TransactionRepository transactionRepository;
    private final TimestampMapper timestampMapper;

    @Override
    public AgencyDueReportDTO getAgencyDueReport(String currency, String sortDir, int page, int size) {
        String currencyUpper = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : null;

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, "mother_user_balance"));

        Page<BusinessEntity> businessPage = businessRepository.findAgenciesDueWithCurrency(currencyUpper, pageable);

        Double totalDue = businessRepository.sumAgencyDueWithCurrency(currencyUpper);
        if (totalDue != null && totalDue < 0) {
            totalDue = Math.abs(totalDue);
        }

        List<Object[]> grouped = businessRepository.sumAgencyDueGroupedByCurrency(currencyUpper);
        Map<String, Double> totalsByCurrency = new LinkedHashMap<>();
        for (Object[] row : grouped) {
            String cur = (String) row[0];
            Double sum = row[1] != null ? Math.abs(((Number) row[1]).doubleValue()) : 0.0;
            if (cur != null) totalsByCurrency.put(cur, sum);
        }

        Page<AgencyDueItemDTO> records = businessPage.map(this::toItemDTO);

        return AgencyDueReportDTO.builder()
                .totalDueAmount(totalDue != null ? totalDue : 0.0)
                .totalCount(businessPage.getTotalElements())
                .totalsByCurrency(totalsByCurrency)
                .records(records)
                .build();
    }

    @Override
    public AgencyDueReportDTO getAgencyCreditReport(String currency, String sortDir, int page, int size) {
        String currencyUpper = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : null;

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, "mother_user_balance"));

        Page<BusinessEntity> businessPage = businessRepository.findAgenciesCreditWithCurrency(currencyUpper, pageable);

        Double totalCredit = businessRepository.sumAgencyCreditWithCurrency(currencyUpper);

        List<Object[]> grouped = businessRepository.sumAgencyCreditGroupedByCurrency(currencyUpper);
        Map<String, Double> totalsByCurrency = new LinkedHashMap<>();
        for (Object[] row : grouped) {
            String cur = (String) row[0];
            Double sum = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            if (cur != null) totalsByCurrency.put(cur, sum);
        }

        Page<AgencyDueItemDTO> records = businessPage.map(this::toItemDTO);

        return AgencyDueReportDTO.builder()
                .totalDueAmount(totalCredit != null ? totalCredit : 0.0)
                .totalCount(businessPage.getTotalElements())
                .totalsByCurrency(totalsByCurrency)
                .records(records)
                .build();
    }

    private AgencyDueItemDTO toItemDTO(BusinessEntity b) {
        User motherUser = b.getMotherUser();
        String currency = motherUser != null ? motherUser.getCurrency() : null;
        Double balance  = motherUser != null ? motherUser.getBalance()  : null;

        // Load all DEBIT transactions for this agency's mother user
        List<TransactionDto> debitTxns = List.of();
        if (motherUser != null) {
            List<Transaction> raw = transactionRepository
                    .findAllByUserIdAndTypeInUnpaged(motherUser.getId(), DEBIT_TYPES);
            debitTxns = raw.stream().map(this::toTransactionDto).toList();
        }

        return AgencyDueItemDTO.builder()
                .businessId(b.getId())
                .companyName(b.getCompanyName())
                .companyEmail(b.getCompanyEmail())
                .companyPhone(b.getCompanyPhone())
                .balance(balance)               // mother user's wallet balance
                .creditLimit(b.getCreditLimit())
                .currency(currency)
                .createdAt(timestampMapper.toRequestUserTime(b.getCreatedAt(), b.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(b.getUpdatedAt(), b.getUpdatedTimeOffset() != null ? b.getUpdatedTimeOffset() : b.getCreatedTimeOffset()))
                .debitTransactions(debitTxns)
                .build();
    }

    private TransactionDto toTransactionDto(Transaction t) {
        return TransactionDto.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .convertedAmount(t.getConvertedAmount())
                .description(t.getDescription())
                .userId(t.getUserId())
                .createdBy(t.getCreatedBy())
                .createdAt(timestampMapper.createdAt(t))
                .build();
    }
}
