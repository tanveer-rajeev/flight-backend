package com.aerionsoft.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CountriesDto {
    @NotBlank(message = "Country code must not be blank")
    private String countryCode;

    @NotBlank(message = "Country name must not be blank")
    private String countryName;
}
