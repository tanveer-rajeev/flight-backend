package com.aerionsoft.application.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsResponse {

    private Long id;
    private List<String> phoneNumbers;
    private String message;
    private String status;
    private String errorMessage;
}
