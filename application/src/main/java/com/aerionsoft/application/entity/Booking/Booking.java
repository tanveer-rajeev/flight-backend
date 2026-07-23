package com.aerionsoft.application.entity.Booking;

import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.HasCreatedUserTimestamp;
import com.aerionsoft.application.entity.HasUpdatedUserTimestamp;
import com.aerionsoft.application.entity.converter.BookingStatusAttributeConverter;
import com.aerionsoft.application.entity.converter.ProviderAttributeConverter;
import com.aerionsoft.application.entity.listener.UserTimestampListener;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.BookingType;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.enums.booking.TripType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(UserTimestampListener.class)
public class Booking implements HasCreatedUserTimestamp, HasUpdatedUserTimestamp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = ProviderAttributeConverter.class)
    @Column(name = "provider_name", nullable = false)
    private Provider providerName;

    @Column(name = "booking_class", nullable = false)
    private String bookingClass;

    @Enumerated(EnumType.STRING)
    private BookingType type;

    private OffsetDateTime bookingDate;

    private String pnr;
    private String ticketNo;

    private String agent;
    private String customer;

    @Column(length = 2000)
    private String description;

    private String airline;

    @Convert(converter = BookingStatusAttributeConverter.class)
    private BookingStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(name = "ticketing_time")
    private LocalDateTime ticketingTime;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @Column(name = "ticketing_time_offset", length = 32)
    private String ticketingTimeOffset;

    private String bookingPrice;

    @Column(name = "exchange_currency_rate")
    private String exchangeCurrencyRate;

    @Column(name = "exchange_currency")
    private String exchangeCurrency;

    // Original price from GDS before markup (for tracking/reporting)
    @Column(name = "original_price")
    private String originalPrice;

    // Supplier buy price after commission provision (for invoice/reporting)
    @Column(name = "buy_price")
    private String buyPrice;

    // Profit/loss in USD: sell price − buy price
    @Column(name = "profit_loss")
    private String profitLoss;

    // Markup amount applied (for tracking/reporting)
    @Column(name = "markup_amount")
    private String markupAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Column to track if another user (child) is acting on behalf of the creator (parent)
    @Column(name = "acting_user_id")
    private Long actingUserId;

    // Store traveller IDs as comma-separated string for simplicity
    @Column(name = "traveller_ids")
    private String travellerIds;

    // Keep the single traveller relationship for backward compatibility
    @Column(name = "traveller_id", nullable = true)
    private Long travellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traveller_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Traveller traveller;

    @Column(length = 2000) // If error reason can be long
    private String reason;

    private TripType tripType;

    private String channel;

    private String lastPaymentDate;

    private String createdByName;

    @Builder.Default
    @Column(name = "is_booking_allowed")
    private boolean bookingAllowed = true;

    @Builder.Default
    @Column(name = "is_ticketing_allowed")
    private boolean ticketingAllowed = true;

    @Column(name = "tax_amount")
    private Double taxAmount;

    @Column(name = "brand_currency")
    private String brandCurrency;

    @Column(name = "brand_exchange_rate")
    private Double brandExchangeRate;

    // Booking reference with prefix FR + yymmdd + sequence
    @Column(name = "booking_reference", unique = true)
    private String bookingReference;

    @Column(name = "bundle_code")
    private String bundleCode;

    @Column(name = "last_payment_date_in_seconds")
    private Long lastPaymentDateInSeconds;

    @Column(name = "airline_pnrs")
    private String airlinePnrs;

    private String providerBookingTime;

    @Column(name = "time_offset", length = 10)
    private String timeOffset;

    @Column(name = "booked_time_offset", length = 10)
    private String bookedTimeOffset;

    @Builder.Default
    @Column(name = "imported_pnr")
    private Boolean importedPnr = false;

    @Builder.Default
    @Column(name = "auto_cancel_failure_count", nullable = false)
    private int autoCancelFailureCount = 0;

    private String sourceType;

    /** GROUP / UMRAH / A2A for group-ticket bookings. */
    @Column(name = "group_ticket_type")
    private String groupTicketType;
}