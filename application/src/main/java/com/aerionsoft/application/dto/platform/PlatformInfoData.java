package com.aerionsoft.application.dto.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private List<String> providersMap;
    private Map<String, String> keyValues;
    private List<String> providers;
}
