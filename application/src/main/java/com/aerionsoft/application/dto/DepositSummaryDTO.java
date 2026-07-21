package com.aerionsoft.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepositSummaryDTO {
    private BigDecimal pendingBdt;
    private BigDecimal pendingInr;
    private BigDecimal pendingUsd;
    private BigDecimal pendingPkr;
    private BigDecimal pendingSar;
    private BigDecimal pendingQar;

    private BigDecimal approvedBdt;
    private BigDecimal approvedInr;
    private BigDecimal approvedUsd;
    private BigDecimal approvedPkr;
    private BigDecimal approvedSar;
    private BigDecimal approvedQar;

    // Optional: convert Double to BigDecimal if JPQL returns Double
    public DepositSummaryDTO(Double pendingBdt, Double pendingInr, Double pendingUsd,
                             Double pendingPkr, Double pendingSar, Double pendingQar,
                             Double approvedBdt, Double approvedInr, Double approvedUsd,
                             Double approvedPkr, Double approvedSar, Double approvedQar) {
        this.pendingBdt = pendingBdt != null ? BigDecimal.valueOf(pendingBdt) : BigDecimal.ZERO;
        this.pendingInr = pendingInr != null ? BigDecimal.valueOf(pendingInr) : BigDecimal.ZERO;
        this.pendingUsd = pendingUsd != null ? BigDecimal.valueOf(pendingUsd) : BigDecimal.ZERO;
        this.pendingPkr = pendingPkr != null ? BigDecimal.valueOf(pendingPkr) : BigDecimal.ZERO;
        this.pendingSar = pendingSar != null ? BigDecimal.valueOf(pendingSar) : BigDecimal.ZERO;
        this.pendingQar = pendingQar != null ? BigDecimal.valueOf(pendingQar) : BigDecimal.ZERO;

        this.approvedBdt = approvedBdt != null ? BigDecimal.valueOf(approvedBdt) : BigDecimal.ZERO;
        this.approvedInr = approvedInr != null ? BigDecimal.valueOf(approvedInr) : BigDecimal.ZERO;
        this.approvedUsd = approvedUsd != null ? BigDecimal.valueOf(approvedUsd) : BigDecimal.ZERO;
        this.approvedPkr = approvedPkr != null ? BigDecimal.valueOf(approvedPkr) : BigDecimal.ZERO;
        this.approvedSar = approvedSar != null ? BigDecimal.valueOf(approvedSar) : BigDecimal.ZERO;
        this.approvedQar = approvedQar != null ? BigDecimal.valueOf(approvedQar) : BigDecimal.ZERO;
    }
}
