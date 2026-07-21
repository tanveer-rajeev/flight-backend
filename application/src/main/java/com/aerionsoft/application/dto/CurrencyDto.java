package com.aerionsoft.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurrencyDto {
    private String name;
    private String code;
    private Double rate;
//    @NotNull(message = "Provider name must not be null")
    private String providerName;
    private String channel;

}
