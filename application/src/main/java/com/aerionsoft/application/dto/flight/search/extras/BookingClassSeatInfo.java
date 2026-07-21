package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingClassSeatInfo {
    private String bookingCode;
    private Integer seatCount;
    private Boolean moreThanNine;
}
