package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.UserBalanceReconciliationDTO;
import com.aerionsoft.application.dto.report.UserBalanceReconciliationReportDTO;
import com.aerionsoft.application.entity.view.UserBalanceReconciliation;
import com.aerionsoft.application.repository.wallet.UserBalanceReconciliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBalanceReconciliationService {

    private final UserBalanceReconciliationRepository repository;

    /**
     * Returns a paginated reconciliation report.
     *
     * @param onlyDiscrepancies if true, only rows where diff_amount != 0 are returned
     * @param page             zero-based page index
     * @param size             page size
     * @param sortBy           field to sort by (default: userId)
     * @param sortDir          "asc" or "desc"
     */
    public UserBalanceReconciliationReportDTO getReport(
            boolean onlyDiscrepancies,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<UserBalanceReconciliation> rawPage = onlyDiscrepancies
                ? repository.findDiscrepancies(pageable)
                : repository.findAll(pageable);

        Page<UserBalanceReconciliationDTO> dtoPage = rawPage.map(this::toDTO);

        return UserBalanceReconciliationReportDTO.builder()
                .totalCount(rawPage.getTotalElements())
                .records(dtoPage)
                .build();
    }

    private UserBalanceReconciliationDTO toDTO(UserBalanceReconciliation r) {
        return UserBalanceReconciliationDTO.builder()
                .userId(r.getUserId())
                .agencyName(r.getAgencyName())
                .totalCredit(r.getTotalCredit())
                .totalDebit(r.getTotalDebit())
                .debCredBalance(r.getDebCredBalance())
                .userBalance(r.getUserBalance())
                .diffAmount(r.getDiffAmount())
                .build();
    }
}

