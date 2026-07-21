package com.aerionsoft.application.gateway.dtos.ngenius;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NGeniusThreeDSResponseDto {
    @JsonProperty("_id")
    private String id;

    @JsonProperty("_links")
    private Links links;

    private String reference;

    private PaymentMethod paymentMethod;

    private String state;

    private Amount amount;

    private String updateDateTime;

    private String outletId;

    private String orderReference;

    private String originIp;

    @JsonProperty("3ds2")
    private ThreeDS2 threeDS2;

    private String mid;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private Link self;

        @JsonProperty("cnp:3ds2-challenge-response")
        private Link cnp3ds2ChallengeResponse;

        @JsonProperty("cnp:3ds2-authentication")
        private Link cnp3ds2Authentication;

        @JsonProperty("cnp:3ds")
        private Link cnp3ds;

        private List<Curie> curies;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Link {
            private String href;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Curie {
            private String name;
            private String href;
            private boolean templated;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentMethod {
        private String expiry;
        private String cardholderName;
        private String name;
        private String pan;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private String currencyCode;
        private int value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThreeDS2 {
        private String eci;
        private String transStatus;
        private String messageVersion;
        private String acsReferenceNumber;
        private String threeDSMethodURL;
        private String threeDSServerTransID;
        private String acsURL;
        private String acsTransID;
        private String directoryServerID;
        private String base64EncodedCReq;
    }
}
