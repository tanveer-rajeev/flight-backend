package com.aerionsoft.application.dto.admin.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionResponse {
    private String sessionId;
    private String sessionUrl;
    private String depositReference;
    private Double amount;
    private String status;
}
