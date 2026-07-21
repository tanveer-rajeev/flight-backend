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
public class UserCreditRequestInput {

    private BigDecimal requestedAmount;

    private String reason;
}

