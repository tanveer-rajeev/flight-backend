package com.aerionsoft.application.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResultDto {
    private String status;
    private String title;
    private String message;
    private String cssClass;
}