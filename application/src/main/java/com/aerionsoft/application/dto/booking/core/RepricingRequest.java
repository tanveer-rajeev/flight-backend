package com.aerionsoft.application.dto.booking.core;

import lombok.Data;

@Data
public class RepricingRequest {
    private String channel;
    private String key;
    private boolean confirmed;
}
