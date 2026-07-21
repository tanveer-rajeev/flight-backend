package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditListItemDTO {

    private Long       businessId;
    private String     companyName;
    private String     currency;
    private String     balance;

    /** Total credit limit configured for this business */
    private BigDecimal creditLimit;
}
