package com.aerionsoft.application.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepricedResponse {
    private String status;
    private String message;
    private Double amount;
    private String key;
}
