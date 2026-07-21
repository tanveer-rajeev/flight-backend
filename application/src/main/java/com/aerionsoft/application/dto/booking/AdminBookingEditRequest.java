package com.aerionsoft.application.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin booking field edit + optional agency transfer.
 * All fields optional except {@code reason}; at least one editable field required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingEditRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    private String pnr;

    /**
     * Sell price in the owning agency currency (or target agency currency when transferring).
     * Converted to USD for storage on the booking.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "bookingPrice must be greater than 0")
    private BigDecimal bookingPrice;

    /**
     * Buy price in the owning agency currency (or target agency currency when transferring).
     * Converted to USD for storage on the booking.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "buyPrice must be zero or greater")
    private BigDecimal buyPrice;

    @Valid
    private List<TravellerNameUpdate> travellers;

    /**
     * Mother agency user id to transfer ownership to.
     * Clears actingUserId on transfer.
     */
    private Long targetUserId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TravellerNameUpdate {
        @NotNull(message = "travellerId is required")
        private Long travellerId;
        private String title;
        private String firstName;
        private String lastName;
    }
}
