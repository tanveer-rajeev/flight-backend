package com.aerionsoft.application.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SslCommerzInitResponse {
    private String tranId;
    private String sessionKey;
    private String redirectUrl;
    private String status;
    private BigDecimal amount;
    private String currency;
    private Long bookingId;        // For booking payments
    private String depositReference; // For wallet deposits
}