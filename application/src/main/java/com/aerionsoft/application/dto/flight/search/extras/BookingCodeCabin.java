package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingCodeCabin {
    private String cabinClass;
    private List<BookingClassSeatInfo> bookingClasses;
}
