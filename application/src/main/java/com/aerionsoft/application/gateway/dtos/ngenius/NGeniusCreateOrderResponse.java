package com.aerionsoft.application.gateway.dtos.ngenius;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NGeniusCreateOrderResponse {
    private String _id;

    @JsonProperty("_links")
    private Links links;

    private String type;
    private MerchantDefinedData merchantDefinedData;
    private String action;
    private Amount amount;
    private String language;
    private MerchantAttributes merchantAttributes;
    private String reference;
    private String outletId;
    private String createDateTime;
    private PaymentMethods paymentMethods;
    private String referrer;
    private MerchantDetails merchantDetails;
    @JsonProperty("isSplitPayment")
    private boolean isSplitPayment;
    @JsonProperty("isSamsungPayV2")
    private boolean isSamsungPayV2;
    @JsonProperty("isSaudiPaymentEnabled")
    private boolean isSaudiPaymentEnabled;
    private PayoutDetails payoutDetails;
    private Object formattedOrderSummary;
    private String formattedAmount;
    private String formattedOriginalAmount;

    @JsonProperty("_embedded")
    private Embedded embedded;


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private Link cancel;

        @JsonProperty("cnp:payment-link")
        private Link cnpPaymentLink;

        @JsonProperty("payment-authorization")
        private Link paymentAuthorization;

        private Link self;

        @JsonProperty("tenant-brand")
        private Link tenantBrand;

        private Link payment;

        @JsonProperty("merchant-brand")
        private Link merchantBrand;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String href;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private String currencyCode;
        private BigDecimal value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MerchantAttributes {
        private String cancelUrl;
        private String redirectUrl;
        private String skipConfirmationPage;
        private String skip3DS;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentMethods {
        private String[] card;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MerchantDetails {
        private String reference;
        private String name;
        private String companyUrl;
        private String email;
        private String mobile;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayoutDetails {
        private String error;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MerchantDefinedData {
        // NGenius returns {}, so this stays empty
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        @JsonProperty("payment")
        private java.util.List<Payment> payment;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payment {

        private String _id;

        @JsonProperty("_links")
        private Links links;

        private String reference;
        private String state;
        private Amount amount;
        private String updateDateTime;
        private String outletId;
        private String orderReference;
    }
}
