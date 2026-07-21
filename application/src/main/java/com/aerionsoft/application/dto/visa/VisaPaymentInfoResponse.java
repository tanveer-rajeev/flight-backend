package com.aerionsoft.application.dto.visa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaPaymentInfoResponse {
    private Long transactionId;
    private Long depositId;
    private String reference;
    private String type;
    private Double amount;
    private String currency;
    private String description;
    private LocalDateTime paidAt;
    private Boolean status;
}
