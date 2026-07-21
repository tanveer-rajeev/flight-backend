package com.aerionsoft.application.dto.client.invoice;

import com.aerionsoft.application.enums.booking.Provider;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierProviderMappingDto {
    private Provider provider;
    /** Platform channel code (e.g. galileo-bd). Omit for provider-wide mapping. */
    private String channel;
}
