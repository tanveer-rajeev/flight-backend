package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.CreditListItemDTO;
import com.aerionsoft.application.dto.report.CreditListReportDTO;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.repository.business.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditListReportServiceImpl implements CreditListReportService {

    private final BusinessRepository businessRepository;

    @Override
    public CreditListReportDTO getCreditList(String currency, Long agencyId, String sortDir, int page, int size) {

        String currencyFilter = (currency != null && !currency.isBlank())
                ? currency.trim().toUpperCase()
                : null;

        // 1. Fetch all businesses with creditLimit > 0
        List<BusinessEntity> all = businessRepository.findByCreditLimitGreaterThan(BigDecimal.valueOf(1));

        // 2. Stream: filter → sort → paginate
        Comparator<BusinessEntity> comparator = "asc".equalsIgnoreCase(sortDir)
                ? Comparator.comparing(BusinessEntity::getCreditLimit)
                : Comparator.comparing(BusinessEntity::getCreditLimit).reversed();

        List<CreditListItemDTO> filtered = all.stream()
                .filter(b -> {
                    if (agencyId != null && !agencyId.equals(b.getId())) return false;
                    if (currencyFilter != null) {
                        User u = b.getMotherUser();
                        if (u == null) return false;
                        String uc = u.getCurrency();
                        if (uc == null || !uc.trim().toUpperCase().equals(currencyFilter)) return false;
                    }
                    return true;
                })
                .sorted(comparator)
                .map(this::toItemDTO)
                .toList();

        long totalCount = filtered.size();

        // 3. Manual pagination
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex   = Math.min(fromIndex + size, filtered.size());
        List<CreditListItemDTO> pageContent = filtered.subList(fromIndex, toIndex);

        Page<CreditListItemDTO> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, size), totalCount);

        return CreditListReportDTO.builder()
                .totalCount(totalCount)
                .records(pageResult)
                .build();
    }

    private CreditListItemDTO toItemDTO(BusinessEntity b) {
        User u = b.getMotherUser();
        return CreditListItemDTO.builder()
                .businessId(b.getId())
                .companyName(b.getCompanyName())
                .currency(u != null ? u.getCurrency() : null)
                .balance(u != null && u.getBalance() != null ? String.valueOf(u.getBalance()) : "0.0")
                .creditLimit(b.getCreditLimit() != null ? b.getCreditLimit() : BigDecimal.ZERO)
                .build();
    }
}
