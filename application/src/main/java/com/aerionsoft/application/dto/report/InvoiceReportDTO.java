package com.aerionsoft.application.dto.report;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceReportDTO {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private BigDecimal amount;
    private Long createdBy;
    private Long updatedBy;
    private String details;
    private String entity;

    // JPQL-safe constructor
    public InvoiceReportDTO(Long id, String title, LocalDateTime createdAt, Number amount, Long createdBy, Long updatedBy, String entity) {
        this.id = id;
        this.title = title;
        this.createdAt = createdAt;
        this.amount = amount == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(amount.doubleValue());
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.entity = entity;
    }
}
