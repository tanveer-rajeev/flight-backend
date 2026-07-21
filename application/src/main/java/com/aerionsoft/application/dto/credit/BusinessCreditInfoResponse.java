package com.aerionsoft.application.dto.credit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessCreditInfoResponse {

    private Long businessId;

    private String businessName;

    private BigDecimal totalCreditGiven;

    private BigDecimal totalCreditUsed;

    private BigDecimal availableCredit;
}
