package com.aerionsoft.application.dto.gateway;

import lombok.*;

@Getter
@Setter
public class NGeniusCredentialDto {
    private Long id;
    private String outletReference;
    private String apiKey;
    private String baseUrl;
    private String cancelUrl;
    private String redirectUrl;
    private Boolean isActive;
}
