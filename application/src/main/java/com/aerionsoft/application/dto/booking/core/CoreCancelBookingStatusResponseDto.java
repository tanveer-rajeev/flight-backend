package com.aerionsoft.application.dto.booking.core;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreCancelBookingStatusResponseDto {
    private String pnr;
    private boolean isCancelled;
    private String message;
}
