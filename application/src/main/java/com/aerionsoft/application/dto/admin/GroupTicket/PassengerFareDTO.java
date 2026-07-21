package com.aerionsoft.application.dto.admin.GroupTicket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassengerFareDTO {
    private String fareBasis;
    private Integer quantity;
    private Integer bookedQuantity;
    private String currency;
    private Double baseFare;
    private Double equivalentBaseFare;
    private Double equivalentTaxes;
    private String publishedFare;
    private Double exchangeRate;
    private Double baggageKg;

}
