package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.enums.client.ManualInvoicePaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SupplierTransactionCreateDTO {
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Ledger id is required")
    private Long ledgerId;

    @NotNull(message = "Supplier id is required")
    private Long supplierId;

    @NotNull(message = "Payment type is required")
    private ManualInvoicePaymentType paymentType;

    private String description;

    private String attachment;

    /** Required when paymentType is BANK */
    private Long depositBankId;
}
