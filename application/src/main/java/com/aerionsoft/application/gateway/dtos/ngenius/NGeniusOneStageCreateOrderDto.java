package com.aerionsoft.application.gateway.dtos.ngenius;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NGeniusOneStageCreateOrderDto {
    private Order order;
    private Payment payment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private String action;
        private Amount amount;
        private String emailAddress;
        private MerchantAttributes merchantAttributes;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Amount {
            private String currencyCode;
            private int value;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MerchantAttributes {
            private String redirectUrl;
            private boolean skipConfirmationPage;
            private boolean skip3DS;
            private String cancelUrl;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private String pan;
        private String expiry;
        private String cvv;
        private String cardholderName;
    }
}
