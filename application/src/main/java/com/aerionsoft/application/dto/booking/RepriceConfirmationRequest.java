package com.aerionsoft.application.dto.booking;

import lombok.Data;

@Data
public class RepriceConfirmationRequest {
    private String transactionId;
    private String channel;
    private String pnr;
}
