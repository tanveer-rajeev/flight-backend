package com.aerionsoft.application.dto.booking;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aerionsoft.application.dto.business.BusinessSimpleDto;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.dto.traveller.TravellerResponse;
import com.aerionsoft.application.enums.booking.BookingClass;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.BookingType;
import com.aerionsoft.application.enums.booking.Provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;
    private Provider providerName;
    private BookingClass bookingClass;
    private BookingType type;
    private OffsetDateTime bookingDate;
    private String pnr;
    private String ticketNo;
    private List<String> ticketNumbers;
    private String description;
    private String airline;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime ticketingTime;
    private Long createdBy;
    private String createdByName;
    private String resultIndex;
    private List<Long> travellerIds;
    private List<TravellerResponse> travellers;
    private UserDto user;
    private String channel;
    private String exchangeCurrencyRate;
    private String exchangeCurrency;

    private String TripType;

    private TravelInformation travelInformation;

    private Long lastPaymentDateInSeconds;

    private List<ExtrasDTO> extras;

    // Price tracking fields for markup reporting
    private String bookingPrice;
    private String originalPrice;
    private String buyPrice;
    private String markupAmount;
    /** Profit/loss in USD (sell price − buy price). */
    private String profitLoss;

    @JsonProperty("isBookingAllowed")
    private boolean isBookingAllowed;
    @JsonProperty("isTicketingAllowed")
    private boolean isTicketingAllowed;

    private Double taxAmount;

    private BusinessSimpleDto business;

    private String brandCurrency;
    private Double brandExchangeRate = 0.0;

    private String bookingReference;

    private String bundleCode;

    private List<PackageBaggageDTO> packageBaggageList;

    private List<String> airlinePnrs;
    private String providerBookingTime;

    private String lastPaymentDate;
    private String lastPaymentTimeForUser;

    private String timeOffset;
    private String bookedTimeOffset;

    private boolean importedPnr;
    private String sourceType;
    private String groupTicketType;

    private static final Pattern OFFSET_PATTERN = Pattern.compile("^([+-])(\\d{1,2}):(\\d{1,2})$");
    private static final DateTimeFormatter FLEXIBLE_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    /* Custom setters to trigger calculation */
    public void setLastPaymentDate(String lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
        calculateUserTime();
    }

    public void setTimeOffset(String timeOffset) {
        this.timeOffset = timeOffset;
        calculateUserTime();
    }

    public void setBookedTimeOffset(String bookedTimeOffset) {
        this.bookedTimeOffset = bookedTimeOffset;
        calculateUserTime();
    }


    /* Time calculation */
    private void calculateUserTime() {

        try {

            if (lastPaymentDate == null || lastPaymentDate.isBlank()) {
                lastPaymentTimeForUser = null;
                return;
            }

            ZoneOffset sourceOffset = parseOffset(bookedTimeOffset);
            ZoneOffset targetOffset = parseOffset(timeOffset);

            if (sourceOffset == null || targetOffset == null) {
                lastPaymentTimeForUser = lastPaymentDate;
                return;
            }

            LocalDateTime paymentDateTime = LocalDateTime.parse(lastPaymentDate, FLEXIBLE_DATE_TIME_FORMATTER);

            OffsetDateTime paymentTime = OffsetDateTime.of(paymentDateTime, sourceOffset);

            OffsetDateTime userTime = paymentTime.withOffsetSameInstant(targetOffset);

            lastPaymentTimeForUser = formatWithOriginalFraction(userTime.toLocalDateTime(), lastPaymentDate);

        } catch (Exception e) {
            lastPaymentTimeForUser = lastPaymentDate;
        }
    }

    private String formatWithOriginalFraction(LocalDateTime dateTime, String originalValue) {
        int fractionIndex = originalValue.indexOf('.');
        if (fractionIndex < 0) {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        }

        String fraction = originalValue.substring(fractionIndex + 1).trim();
        int fractionLength = Math.min(fraction.length(), 9);

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, fractionLength, fractionLength, true)
                .toFormatter();

        return dateTime.format(formatter);
    }

    private ZoneOffset parseOffset(String offset) {
        if (offset == null) {
            return null;
        }

        String normalized = offset.replaceAll("\\s+", "").trim();
        if (normalized.isEmpty()) {
            return null;
        }

        Matcher matcher = OFFSET_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        String sign = matcher.group(1);
        int hours = Integer.parseInt(matcher.group(2));
        int minutes = Integer.parseInt(matcher.group(3));

        if (hours > 18 || minutes > 59) {
            return null;
        }

        return ZoneOffset.of(String.format("%s%02d:%02d", sign, hours, minutes));
    }
}
