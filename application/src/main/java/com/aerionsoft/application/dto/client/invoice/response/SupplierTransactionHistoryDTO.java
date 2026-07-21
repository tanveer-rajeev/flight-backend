package com.aerionsoft.application.dto.client.invoice.response;

import com.aerionsoft.application.dto.CreatorDto;
import com.aerionsoft.application.dto.admin.bank.DepositBankSummaryResponse;
import com.aerionsoft.application.enums.client.ManualInvoicePaymentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupplierTransactionHistoryDTO {
    private Long id;
    private CreatorDto agency;
    private CreatorDto supplier;
    private LedgerShortDTO ledger;
    private Long invoiceId;
    private Long invoiceItemId;
    private BigDecimal paidAmount;
    private BigDecimal payableAmount;
    private String title;
    private String description;
    private LocalDateTime createdDate;
    private ManualInvoicePaymentType paymentType;
    private DepositBankSummaryResponse depositBank;
    private List<SupplierTransactionHistoryDetailDTO> details = new ArrayList<>();

    // Booking info (populated when invoiceItemId is present)
    private BigDecimal originalPrice;
    private BigDecimal bookingPrice;
}
