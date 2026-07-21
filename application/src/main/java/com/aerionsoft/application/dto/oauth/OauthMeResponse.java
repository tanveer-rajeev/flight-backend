package com.aerionsoft.application.dto.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OauthMeResponse {
    @JsonProperty("sub")
    private String subject;

    @JsonProperty("partnerId")
    private String partnerId;

    @JsonProperty("subscription")
    private String subscription;

    @JsonProperty("name")
    private String fullName;

    @JsonProperty("given_name")
    private String firstName;

    @JsonProperty("family_name")
    private String lastName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    @JsonProperty("currency_code")
    private String currencyCode;
}
