package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.ledger.LedgerResponse;
import com.aerionsoft.application.dto.wallet.SourceEnrichment;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.Booking.Traveller;
import com.aerionsoft.application.entity.group.TravelInformation;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.repository.booking.TravelInformationRepository;
import com.aerionsoft.application.repository.booking.TravellerRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BookingTransactionSourceResolver implements TransactionSourceResolver {

    private final BookingRepository bookingRepository;
    private final TravelInformationRepository travelInformationRepository;
    private final TravellerRepository travellerRepository;
    private final TimestampMapper timestampMapper;

    public BookingTransactionSourceResolver(BookingRepository bookingRepository,
                                            TravelInformationRepository travelInformationRepository,
                                            TravellerRepository travellerRepository,
                                            TimestampMapper timestampMapper) {
        this.bookingRepository = bookingRepository;
        this.travelInformationRepository = travelInformationRepository;
        this.travellerRepository = travellerRepository;
        this.timestampMapper = timestampMapper;
    }

    @Override
    public boolean supports(String sourceType) {
        return TransactionSourceType.BOOKING.name().equals(sourceType);
    }

    @Override
    public Map<Long, ?> batchLoad(Collection<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return bookingRepository.findAllById(sourceIds).stream()
                .collect(Collectors.toMap(Booking::getId, Function.identity()));
    }

    @Override
    public SourceEnrichment enrich(Transaction txn, Map<Long, ?> batchCache) {
        if (txn.getSourceId() == null) {
            return SourceEnrichment.empty();
        }
        Object cached = batchCache != null ? batchCache.get(txn.getSourceId()) : null;
        Booking booking = cached instanceof Booking b ? b : bookingRepository.findById(txn.getSourceId()).orElse(null);
        if (booking == null) {
            return SourceEnrichment.empty();
        }

        LedgerResponse.BookingInfo bookingInfo = mapBookingInfo(booking);
        return SourceEnrichment.builder()
                .sourceType(TransactionSourceType.BOOKING.name())
                .sourceId(booking.getId())
                .label(booking.getPnr())
                .detail(booking.getTicketNo())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .bookingInfo(bookingInfo)
                .build();
    }

    public LedgerResponse.BookingInfo mapBookingInfo(Booking booking) {
        String route = null;
        String flightDate = null;
        TravelInformation travelInfo = travelInformationRepository.findByBookingId(booking.getId());
        if (travelInfo != null) {
            if (travelInfo.getOrigin() != null && travelInfo.getDestination() != null) {
                route = travelInfo.getOrigin() + " - " + travelInfo.getDestination();
            }
            flightDate = travelInfo.getDepartureDate();
        }

        Long resolvedTravellerId = booking.getTravellerId();
        if (resolvedTravellerId == null && booking.getTravellerIds() != null && !booking.getTravellerIds().isBlank()) {
            String firstId = booking.getTravellerIds().split(",")[0].trim();
            try {
                resolvedTravellerId = Long.parseLong(firstId);
            } catch (NumberFormatException ignored) {
            }
        }

        String paxName = null;
        String travellerFullName = null;
        if (resolvedTravellerId != null) {
            Optional<Traveller> travellerOpt = travellerRepository.findById(resolvedTravellerId);
            if (travellerOpt.isPresent()) {
                Traveller traveller = travellerOpt.get();
                travellerFullName = (traveller.getFirstName() + " " + traveller.getLastName()).trim();
                paxName = travellerFullName;
            }
        }

        String customer = booking.getCustomer() != null ? booking.getCustomer() : travellerFullName;

        return LedgerResponse.BookingInfo.builder()
                .bookingId(booking.getId())
                .pnr(booking.getPnr())
                .ticketNo(booking.getTicketNo())
                .airline(booking.getAirline())
                .bookingStatus(booking.getStatus() != null ? booking.getStatus().name() : null)
                .bookingClass(booking.getBookingClass())
                .providerName(booking.getProviderName() != null ? booking.getProviderName().name() : null)
                .customer(customer)
                .bookingDate(timestampMapper.toRequestUserTime(booking.getCreatedAt(),
                        booking.getCreatedTimeOffset() != null ? booking.getCreatedTimeOffset() : booking.getTimeOffset()))
                .channel(booking.getChannel())
                .airlineCode(booking.getAirline())
                .bookingRef(booking.getBookingReference())
                .route(route)
                .flightDate(flightDate)
                .paxName(paxName)
                .build();
    }
}
