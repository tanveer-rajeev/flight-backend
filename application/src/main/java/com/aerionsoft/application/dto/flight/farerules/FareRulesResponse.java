package com.aerionsoft.application.dto.flight.farerules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FareRulesResponse {
    private String fareRuleType;
    private List<FareRule> fareRules;
    private String message;
    private String providerCode;
    private String reason;
    private String resultIndex;
    private boolean success;
    private String traceId;
    private List<String> warnings;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FareRule {
        private String airFareDisplayRuleKey;
        private String airline;
        private String destination;
        private String fareBasisCode;
        private String fareFamily;
        private String fareInfoRef;
        private String fareRuleKey;
        private String origin;
        private String providerCode;
        private String ruleNumber;
        private String ruleSource;
        private List<Section> sections;
        private String tariffNumber;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Section {
        private Integer category;
        private String text;
        private String type;
    }
}
