package com.aerionsoft.application.dto.client.invoice.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceDynamicItemResponseDto {
    private Long id;
    private String key;
    private String value;
}
