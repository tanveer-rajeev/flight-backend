package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import com.aerionsoft.application.dto.report.DepositReportDTO;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.wallet.WalletService;
import com.aerionsoft.application.util.FilterRangeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DepositReportServiceImpl implements DepositReportService {

    private final WalletDepositRepository walletDepositRepository;
    private final WalletService walletService;

    @Override
    public DepositReportDTO getDepositReport(DepositType type,
                                              String currency,
                                              LocalDate from,
                                              LocalDate to,
                                              Long agencyId,
                                              int page,
                                              int size) {

        FilterRangeUtil.InstantRange range = FilterRangeUtil.userDateRange(from, to);
        Timestamp startInstant = range.start() != null ? Timestamp.from(range.start()) : null;
        Timestamp endInstant = range.endExclusive() != null ? Timestamp.from(range.endExclusive()) : null;

        String typeStr = type != null ? type.name() : null;

        Currency currencyEnum = Currency.getIndexFromCode(currency);
        Integer currencyOrdinal = currencyEnum != null ? currencyEnum.ordinal() : null;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));

        Page<WalletDeposit> depositPage = walletDepositRepository.findDepositReport(
                typeStr, currencyOrdinal, agencyId, startInstant, endInstant, pageable);

        BigDecimal totalAmount = walletDepositRepository.sumDepositReport(
                typeStr, currencyOrdinal, agencyId, startInstant, endInstant);

        Page<WalletDepositResponse> records = depositPage.map(walletService::mapToResponse1);

        return DepositReportDTO.builder()
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .totalCount(depositPage.getTotalElements())
                .currency(currency != null ? currency.trim().toUpperCase() : null)
                .records(records)
                .build();
    }
}
