package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.credit.CreditLimitHistoryResponse;
import com.aerionsoft.application.dto.report.CreditReportDTO;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.CreditLimitHistory;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.wallet.CreditLimitHistoryRepository;
import com.aerionsoft.application.util.FilterRangeUtil;
import com.aerionsoft.application.util.TimestampMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CreditReportServiceImpl implements CreditReportService {

    private final CreditLimitHistoryRepository creditLimitHistoryRepository;
    private final BusinessRepository businessRepository;
    private final AdminUserRepository adminUserRepository;
    private final TimestampMapper timestampMapper;

    @Override
    public CreditReportDTO getCreditGivenReport(LocalDate from, LocalDate to, String currency, Long agencyId, int page, int size) {
        return buildReport(CreditLimitStatus.CREDIT, from, to, currency, agencyId, page, size);
    }

    @Override
    public CreditReportDTO getCreditUsedReport(LocalDate from, LocalDate to, String currency, Long agencyId, int page, int size) {
        return buildReport(CreditLimitStatus.DEBIT, from, to, currency, agencyId, page, size);
    }

    private CreditReportDTO buildReport(CreditLimitStatus status, LocalDate from, LocalDate to,
                                        String currency, Long agencyId, int page, int size) {
        FilterRangeUtil.InstantRange range = FilterRangeUtil.userDateRange(from, to);
        Timestamp startInstant = range.start() != null ? Timestamp.from(range.start()) : null;
        Timestamp endInstant = range.endExclusive() != null ? Timestamp.from(range.endExclusive()) : null;

        PageRequest pageable = PageRequest.of(page, size);

        Page<CreditLimitHistory> historyPage = creditLimitHistoryRepository.findReportByStatus(
                status.name(), agencyId, currency, startInstant, endInstant, pageable);

        BigDecimal totalAmount = creditLimitHistoryRepository.sumReportAmountByStatus(
                status.name(), agencyId, currency, startInstant, endInstant);

        Page<CreditLimitHistoryResponse> records = historyPage.map(this::toResponse);

        return CreditReportDTO.builder()
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .totalCount(historyPage.getTotalElements())
                .currency(currency)
                .records(records)
                .build();
    }

    private CreditLimitHistoryResponse toResponse(CreditLimitHistory history) {
        BusinessEntity business = businessRepository.findById(history.getBusinessId()).orElse(null);
        String businessName = business != null ? business.getCompanyName() : null;

        String createdByName = null;
        if (history.getCreatedBy() != null) {
            createdByName = adminUserRepository.findById(history.getCreatedBy())
                    .map(AdminUser::getFullName)
                    .orElse(null);
        }

        return CreditLimitHistoryResponse.builder()
                .id(history.getId())
                .businessId(history.getBusinessId())
                .businessName(businessName)
                .amount(history.getAmount())
                .cause(history.getCause())
                .returnDate(history.getReturnDate())
                .createdBy(history.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(timestampMapper.toRequestUserTime(history.getCreatedAt(), history.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(history.getUpdatedAt(), history.getUpdatedTimeOffset() != null ? history.getUpdatedTimeOffset() : history.getCreatedTimeOffset()))
                .adminInstruction(history.getAdminInstruction())
                .status(history.getStatus())
                .balanceBefore(history.getBalanceBefore())
                .balanceAfter(history.getBalanceAfter())
                .build();
    }
}
