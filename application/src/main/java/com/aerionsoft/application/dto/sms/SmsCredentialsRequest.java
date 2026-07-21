package com.aerionsoft.application.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsCredentialsRequest {

    private String providerName;
    private String apiKey;
    private String apiSecret;
    private String senderId;
    private String baseUrl;
}
