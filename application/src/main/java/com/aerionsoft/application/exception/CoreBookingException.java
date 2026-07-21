package com.aerionsoft.application.exception;

import com.aerionsoft.application.dto.booking.core.CoreResponse;
import lombok.Getter;

@Getter
public class CoreBookingException extends RuntimeException {
    private final CoreResponse coreResponse;

    public CoreBookingException(String message, CoreResponse coreResponse) {
        super(message);
        this.coreResponse = coreResponse;
    }

    public CoreBookingException(CoreResponse coreResponse) {
        super(coreResponse.getMessage() != null ? coreResponse.getMessage() : "Core booking failed");
        this.coreResponse = coreResponse;
    }




}
