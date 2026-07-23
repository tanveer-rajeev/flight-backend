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
public class PlatformDetailsDto {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long id;
    private String privacyPolicy;
    private String termsAndConditions;
    private String theme;
    private String logoUrl;
    private String faviconUrl;
    private String description;
    private String icon;
    private String primaryColor;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String address;
}
