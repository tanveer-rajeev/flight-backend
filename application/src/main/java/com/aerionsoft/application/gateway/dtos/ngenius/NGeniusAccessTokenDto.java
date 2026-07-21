package com.aerionsoft.application.gateway.dtos.ngenius;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
public class NGeniusAccessTokenDto {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("refresh_expires_in")
    private int refreshTokenExpiresIn;

    @JsonProperty("token_type")
    private String tokenType;
}
