package com.aerionsoft.application.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SslCommerzCallbackRequest {
    private String tranId;
    private String valId;
    private BigDecimal amount;
    private String cardType;
    private BigDecimal storeAmount;
    private String cardNo;
    private String bankTranId;
    private String status;
    private String tranDate;
    private String error;
    private String currency;
    private String cardIssuer;
    private String cardBrand;
    private String cardSubBrand;
    private String cardIssuerCountry;
    private String cardIssuerCountryCode;
    private String storeId;
    private String verifySign;
    private String verifyKey;
    private String verifySignSha2;
    private String currencyType;
    private BigDecimal currencyAmount;
    private BigDecimal currencyRate;
    private BigDecimal baseFair;
    private String valueA;
    private String valueB;
    private String valueC;
    private String valueD;
    private String subscriptionId;
    private String riskLevel;
    private String riskTitle;
}

