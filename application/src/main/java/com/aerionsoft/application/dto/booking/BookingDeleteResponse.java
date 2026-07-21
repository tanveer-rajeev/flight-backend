package com.aerionsoft.application.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDeleteResponse {
    /** Amount credited back to the agency wallet when a wallet purchase is reversed; null if none. */
    private Double walletRefundAmount;
    /** Booking owner's wallet balance after this operation. */
    private Double balanceAfter;
}
