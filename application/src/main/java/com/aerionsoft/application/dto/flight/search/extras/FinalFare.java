package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinalFare {
    @NotBlank(message = "Currency is required")
    @JsonAlias("currency")
    private String Currency;

    @NotNull(message = "BaseFare is required")
    @JsonAlias("baseFare")
    private Double BaseFare = 0.0;

    @NotNull(message = "Tax is required")
    @JsonAlias("tax")
    private Double Tax = 0.0;

    @NotNull(message = "OfferFare is required")
    @JsonAlias("offerFare")
    private Double OfferFare = 0.0;

    @NotNull(message = "OtherCharges is required")
    @JsonAlias("otherCharges")
    private Double OtherCharges = 0.0;

    @NotNull(message = "Discount is required")
    @JsonAlias("discount")
    private Double Discount = 0.0;

    @NotNull(message = "PublishedFare is required")
    @JsonAlias("publishedFare")
    private Double PublishedFare = 0.0;

    @NotNull(message = "TotalMealCharges is required")
    @JsonAlias("totalMealCharges")
    private Double TotalMealCharges = 0.0;

    @NotBlank(message = "BaseFareCurrency is required")
    @JsonAlias("baseFareCurrency")
    private String BaseFareCurrency;

    @NotBlank(message = "remarks is required")
    private String remarks;

    private Double ait = 0.0;

    @NotNull(message = "fareExchangeRate is required")
    @JsonAlias("fareExchangeRate")
    private Double fareExchangeRate;
}