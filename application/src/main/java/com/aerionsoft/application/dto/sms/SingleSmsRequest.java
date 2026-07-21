package com.aerionsoft.application.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SingleSmsRequest {

    private String phoneNumber;
    private String message;
}