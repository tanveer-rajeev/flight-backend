package com.aerionsoft.application.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentIntentResponse {

    private String clientSecret;
    private String paymentIntentId;
    private String status;
    private Long amount;
    private String currency;
    private Long bookingId;
    private String description;
}
