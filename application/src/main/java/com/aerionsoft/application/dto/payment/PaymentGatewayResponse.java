package com.aerionsoft.application.dto.payment;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentGatewayResponse {
    // Generic gateway identifiers
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String status;
    private String redirectUrl;

    // Card metadata (masked / non-sensitive)
    private String maskedPan;
    private String cardBrand;
    private Integer expiryMonth;
    private Integer expiryYear;

    // Auth / 3DS / reconciliation fields
    private String authorizationCode;
    private String rrn;
    private String eci;
    private String threeDsTransId;
    private String acsTransId;
    private String resultCode;
    private String resultMessage;

    private LocalDateTime gatewayUpdatedAt;
}
