package com.aerionsoft.application.dto.client.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class InvoiceDto {
    private Long travellerId;

    @NotNull(message = "Ledger ID is required")
    private Long ledgerId;

    @NotBlank(message = "Invoice title is required")
    @Size(max = 100)
    private String invoiceTitle;

    @NotBlank(message = "Invoice details is required")
    @Size(max = 1024)
    private String invoiceDetails;

    @NotBlank(message = "Invoice date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private String invoiceDate;

    @NotNull(message = "Invoice amount is required")
    private BigDecimal invoiceAmount;

    @NotNull(message = "Invoice service charge is required")
    private BigDecimal invoiceServiceCharge;

    private BigDecimal invoiceDiscount;

    @NotBlank(message = "Payment method is required")
    @Size(max = 100)
    private String paymentMethod;

    private String document;

    private List<InvoiceItemDto> invoiceItems;
}
