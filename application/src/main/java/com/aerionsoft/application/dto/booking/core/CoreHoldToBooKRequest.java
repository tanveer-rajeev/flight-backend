package com.aerionsoft.application.dto.booking.core;

import lombok.Data;

import java.util.List;

@Data
public class CoreHoldToBooKRequest {

    private String pnr;
    private String providerName;
    private String channel;
    private Double originalPrice;

    private List<Record> records;

}
