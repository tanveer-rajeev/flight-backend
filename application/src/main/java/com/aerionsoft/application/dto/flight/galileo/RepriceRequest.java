package com.aerionsoft.application.dto.flight.galileo;

import lombok.Data;

import java.util.List;

@Data
public class RepriceRequest {
    private String resultIndex;
    private String channel;
    private List<BookingClassSelection> bookingClasses;

    @Data
    public static class BookingClassSelection {
        private Integer leg;
        private String bookingCode;
    }
}
