package com.aerionsoft.application.dto.booking.core;

import com.aerionsoft.application.enums.booking.PassenserEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Record {

    @NotNull(message = "Passenger type is required")
    private PassenserEnum type;
    @NotNull(message = "Passenger count is required")
    private Integer count;

}
