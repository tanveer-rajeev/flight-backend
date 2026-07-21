package com.aerionsoft.application.dto.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NGeniusOneStagePaymentResponseDto {
    @JsonProperty("_id")
    private String id;

    private Links _links;

    private String reference;

    private PaymentMethod paymentMethod;

    private String state;

    private Amount amount;

    private String updateDateTime;

    private String outletId;

    private String orderReference;

    private String authenticationCode;

    private String originIp;

    @JsonProperty("3ds2")
    private ThreeDS2 threeDS2;

    private String mid;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Links {
        private Link self;

        @JsonProperty("cnp:3ds2-challenge-response")
        private Link cnp3ds2ChallengeResp;

        @JsonProperty("cnp:3ds2-authentication")
        private Link cnp3ds2Auth;

        @JsonProperty("cnp:3ds")
        private Link cnp3ds;

        private Curie[] curies;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Link {
            private String href;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Curie {
            private String name;
            private String href;
            private boolean templated;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentMethod {
        private String expiry;
        private String cardholderName;
        private String name;
        private String pan;
        private String cvv;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Amount {
        private String currencyCode;
        private int value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ThreeDS2 {
        private String messageVersion;
        private String threeDSMethodURL;
        private String threeDSServerTransID;
        private String directoryServerID;
    }
}
