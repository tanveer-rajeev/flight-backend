package com.aerionsoft.application.dto.client.invoice.response;

import com.aerionsoft.application.dto.client.invoice.ServiceKeyStepDto;

import java.util.List;

public class InvoiceDynamicServiceResponseDto {
    private String serviceType;
    private List<ServiceKeyStepDto> keys;

    public InvoiceDynamicServiceResponseDto(String serviceType, List<ServiceKeyStepDto> keys) {
        this.serviceType = serviceType;
        this.keys = keys;
    }
}
