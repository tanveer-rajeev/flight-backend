package com.aerionsoft.application.dto.client.invoice.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class InvoiceResponseDto {
    private Long id;
    private Long ledgerId;
    private Long travellerId;
    private String invoiceTitle;
    private BigDecimal invoiceAmount;
    private BigDecimal invoiceServiceCharge;
    private BigDecimal invoiceDiscount;
    private String paymentMethod;
    private String document;
    private String invoiceDetails;
    private Long createdBy;
    private LocalDate invoiceDate;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private String status;
}
