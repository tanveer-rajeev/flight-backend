package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.dto.sabre.TicketingDeadlineRequest;
import com.aerionsoft.application.dto.sabre.TicketingDeadlineResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.repository.booking.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Unified service for handling ticketing deadline retrieval and booking updates.
 * Supports multiple providers (Sabre, Verteil) by resolving the correct API path
 * based on the provider passed at call time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingDeadlineService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${flight_api_url}")
    private String flightApiUrl;

    @Value("${flight_api_key}")
    private String apiKey;

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MINUTES = 4;

    // -------------------------------------------------------------------------
    // Provider → API path mapping
    // -------------------------------------------------------------------------

    /**
     * Returns the provider-specific ticketing-deadline API path segment.
     * e.g. SABRE  → "/api/sabre/flights/ticketing-deadline"
     * VERTEIL → "/api/verteil/flights/ticketing-deadline"
     */
    private String resolveApiPath(Provider provider) {
        return switch (provider) {
            case SABRE -> "/api/sabre/flights/ticketing-deadline";
            case VERTEIL -> "/api/verteil/flights/ticketing-deadline";
            default -> throw new IllegalArgumentException(
                    "Provider " + provider + " does not support ticketing deadline");
        };
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Schedule a ticketing deadline update for a booking.
     * Waits {@value INITIAL_DELAY_MINUTES} minutes then tries up to {@value MAX_RETRIES} times.
     *
     * @param bookingId The booking ID
     * @param provider  The booking provider (SABRE / VERTEIL)
     * @param pnr       The PNR code
     * @param channel   The channel
     */
    @Async("taskExecutor")
    public void scheduleTicketingDeadlineUpdate(Long bookingId, Provider provider, String pnr, String channel) {
        log.info("Scheduling ticketing deadline update — bookingId: {}, provider: {}, PNR: {}, channel: {}",
                bookingId, provider, pnr, channel);

        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MINUTES.sleep(INITIAL_DELAY_MINUTES);

                log.info("Starting ticketing deadline update after {} min — bookingId: {}, provider: {}",
                        INITIAL_DELAY_MINUTES, bookingId, provider);

                // Re-verify booking still exists with expected provider
                Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
                if (bookingOpt.isEmpty()) {
                    log.warn("Booking {} not found — skipping ticketing deadline update", bookingId);
                    return;
                }
                Booking booking = bookingOpt.get();
                if (booking.getProviderName() != provider) {
                    log.warn("Booking {} provider mismatch (expected {}, got {}) — skipping",
                            bookingId, provider, booking.getProviderName());
                    return;
                }

                retryFetchAndUpdate(bookingId, provider, pnr, channel);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Ticketing deadline update interrupted — bookingId: {}", bookingId, e);
            } catch (Exception e) {
                log.error("Unexpected error in ticketing deadline update — bookingId: {}", bookingId, e);
            }
        });
    }

    /**
     * Immediately attempt to update the ticketing deadline for a booking.
     * Looks up the booking from the DB to obtain provider, PNR, and channel.
     *
     * @param bookingId The booking ID
     * @return true if the update succeeded
     */
    public boolean manualUpdateTicketingDeadline(Long bookingId) {
        try {
            Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) {
                log.warn("Booking {} not found", bookingId);
                return false;
            }
            Booking booking = bookingOpt.get();

            Provider provider = booking.getProviderName();
            if (provider != Provider.SABRE && provider != Provider.VERTEIL && provider != Provider.GALILEO) {
                log.warn("Booking {} has unsupported provider {} for ticketing deadline", bookingId, provider);
                return false;
            }
            if (booking.getPnr() == null || booking.getPnr().isEmpty()) {
                log.warn("Booking {} has no PNR", bookingId);
                return false;
            }
            if (booking.getChannel() == null || booking.getChannel().isEmpty()) {
                log.warn("Booking {} has no channel", bookingId);
                return false;
            }
            String pnr = (provider == Provider.SABRE || provider == Provider.GALILEO)
                    ? booking.getPnr()
                    : booking.getAirlinePnrs();

            TicketingDeadlineResponse response =
                    fetchTicketingDeadline(provider, pnr, booking.getChannel());

            if (response != null && response.getSuccess() && response.getData() != null) {
                String deadline = response.getData().getTicketingDeadline();
                if (deadline != null && !deadline.isBlank()) {
                    updateBookingLastPaymentDate(bookingId, deadline, response.getData().getSecondsUntilDeadline(), response.getData().getBookedTimeOffset());
                    log.info("Manually updated lastPaymentDate — bookingId: {}, deadline: {}", bookingId, deadline);
                    return true;
                }
                log.warn("Ticketing deadline is null or blank — bookingId: {}", bookingId);
            }
            return false;

        } catch (Exception e) {
            log.error("Error in manual ticketing deadline update — bookingId: {}: {}", bookingId, e.getMessage(), e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void retryFetchAndUpdate(Long bookingId, Provider provider, String pnr, String channel) {
        boolean success = false;
        int attempts = 0;

        while (!success && attempts < MAX_RETRIES) {
            attempts++;
            log.info("Attempt {}/{} — provider: {}, PNR: {}", attempts, MAX_RETRIES, provider, pnr);
            try {
                TicketingDeadlineResponse response = fetchTicketingDeadline(provider, pnr, channel);
                if (response != null && response.getSuccess() && response.getData() != null) {
                    String deadline = response.getData().getTicketingDeadline();
                    if (deadline != null && !deadline.isBlank()) {
                        updateBookingLastPaymentDate(bookingId, deadline, response.getData().getSecondsUntilDeadline(), response.getData().getBookedTimeOffset());
                        success = true;
                        log.info("Updated lastPaymentDate — bookingId: {}, deadline: {}", bookingId, deadline);
                    } else {
                        log.warn("Deadline null/blank — PNR: {}, attempt: {}", pnr, attempts);
                    }
                } else {
                    log.warn("Invalid response on attempt {} — PNR: {}", attempts, pnr);
                }
            } catch (Exception e) {
                log.error("Error on attempt {} — PNR: {}: {}", attempts, pnr, e.getMessage(), e);
            }

            if (!success && attempts < MAX_RETRIES) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (!success) {
            log.error("Failed to update ticketing deadline — bookingId: {} after {} attempts", bookingId, MAX_RETRIES);
        }
    }

    /**
     * Call the provider-specific ticketing-deadline API.
     *
     * @param provider The provider
     * @param pnr      The PNR
     * @param channel  The channel
     * @return TicketingDeadlineResponse or null on failure
     */
    private TicketingDeadlineResponse fetchTicketingDeadline(Provider provider, String pnr, String channel) {
        String url = flightApiUrl + "/api/flights/ticketing-deadline?channel=" + channel;

        TicketingDeadlineRequest request = TicketingDeadlineRequest.builder().pnr(pnr).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        HttpEntity<TicketingDeadlineRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<TicketingDeadlineResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, TicketingDeadlineResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Fetched ticketing deadline — provider: {}, PNR: {}", provider, pnr);
                return response.getBody();
            }
            log.warn("Non-OK status {} — provider: {}, PNR: {}", response.getStatusCode(), provider, pnr);
            return null;
        } catch (Exception e) {
            log.error("Error calling ticketing deadline API — provider: {}, PNR: {}: {}", provider, pnr, e.getMessage(), e);
            throw e;
        }
    }

    private void updateBookingLastPaymentDate(Long bookingId, String ticketingDeadline, Long secondsUntilDeadline, String bookedTimeOffset) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            booking.setLastPaymentDate(ticketingDeadline);
            booking.setLastPaymentDateInSeconds(secondsUntilDeadline);
            booking.setUpdatedAt(UserDateTimeUtil.now());
            booking.setBookedTimeOffset(bookedTimeOffset);
            bookingRepository.save(booking);
            log.info("Saved lastPaymentDate — bookingId: {}, deadline: {}", bookingId, ticketingDeadline);
        } else {
            log.warn("Booking {} not found when saving lastPaymentDate", bookingId);
        }
    }
}

