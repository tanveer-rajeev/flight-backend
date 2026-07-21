package com.aerionsoft.application.dto.client.invoice.response;

import com.aerionsoft.application.dto.CreatorDto;
import com.aerionsoft.application.entity.Booking.Traveller;
import com.aerionsoft.application.entity.client.Ledger;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class InvoiceFullResponseDTO {
    private Long id;
    private Ledger ledger;
    private Traveller traveller;
    private String invoiceTitle;
    private BigDecimal invoiceAmount;
    private BigDecimal invoiceServiceCharge;
    private BigDecimal invoiceDiscount;
    private String paymentMethod;
    private String document;
    private String invoiceDetails;
    private CreatorDto createdBy;
    private LocalDateTime createdAt;
    private CreatorDto updatedBy;
    private LocalDateTime updatedAt;
    private String status;

    private List<InvoiceItemResponseDto> invoiceItems;
}
