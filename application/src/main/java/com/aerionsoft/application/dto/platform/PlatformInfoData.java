package com.aerionsoft.application.dto.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformInfoData {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<String> providersMap;
    private Map<String, String> keyValues;
    private PlatformDto platform;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<String> providers;
    private List<Object> markupPlans;
}
