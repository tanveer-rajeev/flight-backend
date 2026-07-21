package com.aerionsoft.application.service.report;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.accounthead.AccountHeadShortDTO;
import com.aerionsoft.application.dto.report.AccountHeadReportResponseDTO;
import com.aerionsoft.application.dto.report.AccountHeadReportResponseWithTotalDTO;
import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.entity.client.InvoiceItem;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.expense.ExpenseDetail;
import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.repository.finance.AccountHeadRepository;
import com.aerionsoft.application.repository.expense.ExpenseDetailRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.client.InvoiceItemRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AccountHeadReportService {

    private final ExpenseDetailRepository expenseDetailRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final UserRepository userRepository;
    private final AccountHeadRepository accountHeadRepository;
    private final TimestampMapper timestampMapper;

    public AccountHeadReportService(
            ExpenseDetailRepository expenseDetailRepository,
            InvoiceItemRepository invoiceItemRepository,
            UserRepository userRepository,
            AccountHeadRepository accountHeadRepository,
            TimestampMapper timestampMapper
    ) {
        this.expenseDetailRepository = expenseDetailRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.userRepository = userRepository;
        this.accountHeadRepository = accountHeadRepository;
        this.timestampMapper = timestampMapper;
    }

    @Transactional
    public AccountHeadReportResponseWithTotalDTO getAccountHeadReport(
            String provider,
            Long authUserId,
            LocalDate from,
            LocalDate to,
            AccountHeadType type,
            Long accountHeadId,
            int page,
            int size
    ) {

        boolean isAdmin = provider.equalsIgnoreCase("admin");

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Long agencyId = null;

        if (!isAdmin) {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));

            agencyId = user.getParentUser() != null
                    ? user.getParentUser().getId()
                    : user.getId();
        }

        List<AccountHead> accountHeads;

        // ACCOUNT HEADS
        if (accountHeadId != null) {
            AccountHead accountHead = accountHeadRepository.findById(accountHeadId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account head"));

            accountHeads = List.of(accountHead);
        } else {
            accountHeads = (type != null)
                    ? accountHeadRepository.findByType(type)
                    : accountHeadRepository.findAll();
        }


        AccountHeadReportResponseWithTotalDTO accountHeadReportResponseWithTotalDTO = new AccountHeadReportResponseWithTotalDTO();

        if (accountHeads.isEmpty()) {
            return accountHeadReportResponseWithTotalDTO;
        }


        List<Long> accountHeadIds = accountHeads.stream()
                .map(AccountHead::getId)
                .toList();

        BigDecimal totalExpense = expenseDetailRepository.getTotalItemAmountByAccountHeadIds(accountHeadIds);
        BigDecimal totalIncome = invoiceItemRepository.getTotalInvoiceRevenueByAccountHeadIds(accountHeadIds);

        // FETCH DATA
        List<InvoiceItem> invoiceItems = invoiceItemRepository
                .findByAccountHeadIdIn(accountHeadIds)
                .stream()
                .filter(ii -> timestampMapper.isInUserDateRange(
                        ii.getInvoice().getCreatedAt(),
                        ii.getInvoice().getCreatedTimeOffset(),
                        from,
                        to))
                .toList();

        List<ExpenseDetail> expenseDetails = expenseDetailRepository
                .findByAccountHeadIdIn(accountHeadIds)
                .stream()
                .filter(ed -> timestampMapper.isInUserDateRange(
                        ed.getExpense().getCreatedAt(),
                        ed.getExpense().getCreatedTimeOffset(),
                        from,
                        to))
                .toList();

        List<AccountHeadReportResponseDTO> mergedInvoiceAndExpense = new ArrayList<>();

        for (InvoiceItem ii : invoiceItems) {
            BigDecimal amount = ii.getSellPrice()
                    .multiply(BigDecimal.valueOf(ii.getQuantity()))
                    .subtract(ii.getBuyPrice()
                            .multiply(BigDecimal.valueOf(ii.getQuantity()))
                    );

            mergedInvoiceAndExpense.add(new AccountHeadReportResponseDTO(
                    ii.getInvoice().getId(),
                    new AccountHeadShortDTO(ii.getAccountHead().getId(), ii.getAccountHead().getAccountHeadTitle()),
                    ii.getAccountHead().getType(),
                    ii.getInvoice().getInvoiceTitle(),
                    amount,
                    ii.getDocument(),
                    timestampMapper.toRequestUserTime(ii.getInvoice().getCreatedAt(), ii.getInvoice().getCreatedTimeOffset()),
                    timestampMapper.toRequestUserTime(ii.getInvoice().getUpdatedAt(),
                            ii.getInvoice().getUpdatedTimeOffset() != null ? ii.getInvoice().getUpdatedTimeOffset() : ii.getInvoice().getCreatedTimeOffset())
            ));
        }

        // Merge expense details
        for (ExpenseDetail ed : expenseDetails) {
            mergedInvoiceAndExpense.add(new AccountHeadReportResponseDTO(
                    ed.getExpense().getId(),
                    new AccountHeadShortDTO(ed.getAccountHead().getId(), ed.getAccountHead().getAccountHeadTitle()),
                    ed.getAccountHead().getType(),
                    ed.getExpense().getExpenseTitle(),
                    ed.getItemAmount(),
                    ed.getItemDescription(),
                    timestampMapper.toRequestUserTime(ed.getExpense().getCreatedAt(), ed.getExpense().getCreatedTimeOffset()),
                    timestampMapper.toRequestUserTime(ed.getExpense().getUpdatedAt(),
                            ed.getExpense().getUpdatedTimeOffset() != null ? ed.getExpense().getUpdatedTimeOffset() : ed.getExpense().getCreatedTimeOffset())
            ));
        }

        mergedInvoiceAndExpense.sort(Comparator.comparing(AccountHeadReportResponseDTO::getCreatedDate).reversed());


        // PAGINATION
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mergedInvoiceAndExpense.size());

        List<AccountHeadReportResponseDTO> pageContent =
                start > mergedInvoiceAndExpense.size() ? List.of() : mergedInvoiceAndExpense.subList(start, end);

        accountHeadReportResponseWithTotalDTO.setTotalExpense(totalExpense);
        accountHeadReportResponseWithTotalDTO.setTotalIncome(totalIncome);
        accountHeadReportResponseWithTotalDTO.setAccountHeadReportResponseDTO(new PageImpl<>(pageContent, pageable, mergedInvoiceAndExpense.size()));

        return accountHeadReportResponseWithTotalDTO;
    }
}
