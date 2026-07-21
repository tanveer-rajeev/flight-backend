package com.aerionsoft.application.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SslCommerzValidateRequest {
    @NotBlank(message = "Transaction ID is required")
    private String tranId;
    
    private String valId;
    private String status;
    private String rawResponse;
}