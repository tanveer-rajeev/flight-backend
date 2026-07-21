package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.ExpenseSpecification;
import com.aerionsoft.application.dto.report.InvoiceReportDTO;
import com.aerionsoft.application.dto.report.InvoiceReportWithTotalDTO;
import com.aerionsoft.application.dto.report.InvoiceSpecification;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.expense.ExpenseDetail;
import com.aerionsoft.application.repository.expense.ExpenseRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.repository.client.InvoiceRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final  ExpenseRepository expenseRepository;
    private final  UserRepository userRepository;
    private final TimestampMapper timestampMapper;

    public ReportService(InvoiceRepository invoiceRepository, ExpenseRepository expenseRepository, UserRepository userRepository, TimestampMapper timestampMapper) {
        this.invoiceRepository = invoiceRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.timestampMapper = timestampMapper;
    }

    public InvoiceReportWithTotalDTO getInvoiceExpenseDetail(
            String provider,
            Long authUserId,
            int page,
            int size,
            LocalDate from,
            LocalDate to
    ) {

        boolean isAdmin = "admin".equalsIgnoreCase(provider);

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

        List<InvoiceReportDTO> reports = new ArrayList<>();

        // INVOICES
        invoiceRepository
                .findAll(InvoiceSpecification.filterBy(agencyId, from, to))
                .forEach(invoice -> {

                    BigDecimal totalAmount = invoice.getInvoiceItems()
                            .stream()
                            .map(ii ->
                                    ii.getSellPrice()
                                            .multiply(BigDecimal.valueOf(ii.getQuantity()))
                                            .subtract(
                                                    ii.getBuyPrice()
                                                            .multiply(BigDecimal.valueOf(ii.getQuantity()))
                                            )
                            )
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Handle null values safely
                    BigDecimal discount = invoice.getInvoiceDiscount() != null
                            ? invoice.getInvoiceDiscount()
                            : BigDecimal.ZERO;

                    BigDecimal serviceCharge = invoice.getInvoiceServiceCharge() != null
                            ? invoice.getInvoiceServiceCharge()
                            : BigDecimal.ZERO;

                    totalAmount = totalAmount.subtract(discount);
                    totalAmount = totalAmount.add(serviceCharge);

                    reports.add(new InvoiceReportDTO(
                            invoice.getId(),
                            invoice.getInvoiceTitle(),
                            timestampMapper.toRequestUserTime(invoice.getCreatedAt(), invoice.getCreatedTimeOffset()),
                            totalAmount,
                            invoice.getCreatedBy(),
                            invoice.getUpdatedBy(),
                            "Invoice"
                    ));
                });

        // EXPENSES
        expenseRepository
                .findAll(ExpenseSpecification.filterBy(agencyId, from, to))
                .forEach(expense -> {

                    BigDecimal totalAmount = expense.getExpenseDetails()
                            .stream()
                            .map(ExpenseDetail::getItemAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    reports.add(new InvoiceReportDTO(
                            expense.getId(),
                            expense.getExpenseTitle(),
                            timestampMapper.toRequestUserTime(expense.getCreatedAt(), expense.getCreatedTimeOffset()),
                            totalAmount,
                            expense.getCreatedBy(),
                            expense.getUpdatedBy(),
                            "Expense"
                    ));
                });

        // SORT
        reports.sort(Comparator.comparing(InvoiceReportDTO::getCreatedAt).reversed());

        BigDecimal totalExpense = expenseRepository.getTotalExpenseAmount();
        BigDecimal totalIncome = invoiceRepository.getTotalInvoiceRevenue(agencyId);

        // PAGINATION
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), reports.size());

        List<InvoiceReportDTO> pageContent =
                start > reports.size() ? List.of() : reports.subList(start, end);

        InvoiceReportWithTotalDTO reportWithTotal = new InvoiceReportWithTotalDTO();
        reportWithTotal.setTotalExpense(totalExpense);
        reportWithTotal.setTotalIncome(totalIncome);
        reportWithTotal.setInvoiceReport(new PageImpl<>(pageContent, pageable, reports.size()));

        return reportWithTotal;
    }

}
