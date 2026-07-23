package com.aerionsoft.application.dto.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformDto {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long id;
    private String name;
    private String domain;
    private String slug;
    private Boolean isActive;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;
    private PlatformDetailsDto platformDetails;
}
