package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.dto.booking.BookingTimelineDTO;
import com.aerionsoft.application.dto.flight.MarkupContext;
import com.aerionsoft.application.entity.Booking.BookingTimeline;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.audit.ActorType;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.util.ActorContext;
import com.aerionsoft.application.repository.booking.BookingTimelineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookingTimelineService {

    @Autowired
    private BookingTimelineRepository bookingTimelineRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    /**
     * Record a timeline event for a booking.
     */
    public void record(Long bookingId, BookingStatus status, BookingStatus previousStatus,
                       String pnr, String ticketNo, String reason,
                       Long actorId, String actorName, String actorType) {
        recordJourney(bookingId, null, null, null, status, previousStatus,
                pnr, ticketNo, reason, null, actorId, actorName, actorType);
    }

    /**
     * Convenience overload for SYSTEM-generated booking events (no actor).
     */
    public void recordSystem(Long bookingId, BookingStatus status, BookingStatus previousStatus,
                             String pnr, String ticketNo, String reason) {
        record(bookingId, status, previousStatus, pnr, ticketNo, reason, null, "SYSTEM", "SYSTEM");
    }

    /**
     * Record a pre-booking or in-journey flight step (search, validation, add-to-cart, etc.).
     * Never throws — failures here must not break the main API flow.
     */
    public void recordFlightStep(String sessionId, String resultIndex, String providerName,
                                 BookingStatus status, boolean successful, String reason) {
        SessionActor actor = resolveSessionActor();
        recordJourney(null, sessionId, resultIndex, providerName, status, null,
                null, null, reason, successful, actor.id(), actor.name(), actor.type());
    }

    /**
     * Record a flight step linked to an existing booking (book, ticket issue, status changes).
     */
    public void recordFlightStep(Long bookingId, String sessionId, String resultIndex, String providerName,
                                 BookingStatus status, BookingStatus previousStatus,
                                 boolean successful, String reason, String pnr, String ticketNo) {
        SessionActor actor = resolveSessionActor();
        if (actor.id() == null) {
            actor = new SessionActor(null, "SYSTEM", "SYSTEM");
        }
        recordJourney(bookingId, sessionId, resultIndex, providerName, status, previousStatus,
                pnr, ticketNo, reason, successful, actor.id(), actor.name(), actor.type());
    }

    private void recordJourney(Long bookingId, String sessionId, String resultIndex, String providerName,
                               BookingStatus status, BookingStatus previousStatus,
                               String pnr, String ticketNo, String reason, Boolean successful,
                               Long actorId, String actorName, String actorType) {
        try {
            boolean stepSuccessful = successful != null
                    ? successful
                    : status != BookingStatus.FAILED
                    && status != BookingStatus.SEARCH_FAILED
                    && status != BookingStatus.VALIDATION_FAILED
                    && status != BookingStatus.BUNDLE_VALIDATION_FAILED
                    && status != BookingStatus.ADD_TO_CART_FAILED
                    && status != BookingStatus.REJECTED;

            BookingTimeline timeline = BookingTimeline.builder()
                    .bookingId(bookingId)
                    .sessionId(sessionId)
                    .resultIndex(resultIndex)
                    .providerName(providerName)
                    .successful(stepSuccessful)
                    .status(status)
                    .previousStatus(previousStatus)
                    .title(buildTitle(status))
                    .description(buildDescription(status, previousStatus, stepSuccessful))
                    .reason(reason)
                    .pnr(pnr)
                    .ticketNo(ticketNo)
                    .actorId(actorId)
                    .actorName(actorName)
                    .actorType(actorType != null ? actorType : "SYSTEM")
                    .createdAt(UserDateTimeUtil.now())
                    .build();
            bookingTimelineRepository.saveAndFlush(timeline);
        } catch (Exception e) {
            log.warn("Failed to record booking timeline step status={} sessionId={} bookingId={}: {}",
                    status, sessionId, bookingId, e.getMessage());
        }
    }

    /**
     * Get the full timeline for a booking in chronological order.
     */
    public List<BookingTimelineDTO> getTimeline(Long bookingId) {
        return bookingTimelineRepository
                .findByBookingIdOrderByCreatedAtAsc(bookingId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all journey steps for a search session (search → validation → cart → book).
     */
    public List<BookingTimelineDTO> getTimelineBySession(String sessionId) {
        return bookingTimelineRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get recent flight-related activity for a user (across sessions).
     */
    public List<BookingTimelineDTO> getTimelineByUser(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return bookingTimelineRepository
                .findByActorIdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private BookingTimelineDTO toDTO(BookingTimeline t) {
        return BookingTimelineDTO.builder()
                .id(t.getId())
                .bookingId(t.getBookingId())
                .sessionId(t.getSessionId())
                .resultIndex(t.getResultIndex())
                .providerName(t.getProviderName())
                .successful(t.getSuccessful())
                .status(t.getStatus())
                .previousStatus(t.getPreviousStatus())
                .title(t.getTitle())
                .description(t.getDescription())
                .reason(t.getReason())
                .pnr(t.getPnr())
                .ticketNo(t.getTicketNo())
                .actorId(t.getActorId())
                .actorName(t.getActorName())
                .actorType(t.getActorType())
                .createdAt(timestampMapper.toRequestUserTime(t.getCreatedAt(), t.getCreatedTimeOffset()))
                .build();
    }

    private String resolveActorName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse("User #" + userId);
    }

    private String resolveAdminName(Long adminId) {
        if (adminId == null) {
            return null;
        }
        return adminUserRepository.findById(adminId)
                .map(admin -> admin.getFullName())
                .orElse("Admin #" + adminId);
    }

    private SessionActor resolveSessionActor() {
        ActorContext actor = ActorContext.current();
        if (actor.getType() == ActorType.ADMIN && actor.getId() != null) {
            return new SessionActor(actor.getId(), resolveAdminName(actor.getId()), "ADMIN");
        }

        MarkupContext ctx = extractMarkupContext();
        if (ctx.getUserId() != null) {
            return new SessionActor(
                    ctx.getUserId(),
                    resolveActorName(ctx.getUserId()),
                    ctx.isAgent() ? "AGENT" : "USER");
        }

        return new SessionActor(null, null, "GUEST");
    }

    private record SessionActor(Long id, String name, String type) {
    }

    private MarkupContext extractMarkupContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return MarkupContext.guest();
        }
        try {
            Object principal = authentication.getPrincipal();
            String email = null;
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            }
            if (email != null) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    if (user.isAgency()) {
                        Long businessId = user.getBusiness() != null ? user.getBusiness().getId() : null;
                        return MarkupContext.agent(user.getId(), businessId);
                    }
                    return MarkupContext.authenticatedUser(user.getId());
                }
            }
        } catch (Exception ignored) {
            // fall through to guest
        }
        return MarkupContext.guest();
    }

    private String buildTitle(BookingStatus status) {
        if (status == null) return "Status Updated";
        return switch (status) {
            case PROCESS -> "Booking Processing";
            case PNR -> "PNR Assigned";
            case ON_HOLD -> "Booking On Hold";
            case BOOK -> "Booking Initiated";
            case CONFIRMED -> "Booking Confirmed";
            case TICKET_ISSUED -> "Ticket Issued";
            case TICKETED -> "Ticket Issued";
            case COMPLETED -> "Booking Completed";
            case CANCELLED -> "Booking Cancelled";
            case TICKET_CANCELLED -> "Ticket Cancelled";
            case VOID -> "Booking Voided";
            case REFUND -> "Refund Processed";
            case REJECTED -> "Booking Rejected";
            case FAILED -> "Booking Failed";
            case REPRICE -> "Price Updated";
            case VALIDATION_PROCESS -> "Validating Price";
            case VALIDATION_SUCCESS -> "Price Validated";
            case VALIDATION_FAILED -> "Price Validation Failed";
            case VALIDATION_PRICE_CHANGED -> "Price Changed";
            case SEARCH -> "Flight Search";
            case SEARCH_FAILED -> "Flight Search Failed";
            case BUNDLE_VALIDATION_SUCCESS -> "Bundle Validated";
            case BUNDLE_VALIDATION_FAILED -> "Bundle Validation Failed";
            case ADD_TO_CART -> "Added to Cart";
            case ADD_TO_CART_FAILED -> "Add to Cart Failed";
            default -> "Status: " + status.name();
        };
    }

    private String buildDescription(BookingStatus status, BookingStatus previousStatus, boolean successful) {
        if (status == null) return "";
        String prev = previousStatus != null ? " from " + previousStatus.name() : "";
        String outcome = successful ? "" : " (failed)";
        return switch (status) {
            case PROCESS -> "Booking request is being processed.";
            case PNR -> "PNR has been assigned" + prev + ".";
            case ON_HOLD -> "Booking is placed on hold" + prev + ".";
            case BOOK -> "Booking has been initiated.";
            case CONFIRMED -> "Booking has been confirmed" + prev + ".";
            case TICKET_ISSUED, TICKETED -> "Ticket has been issued successfully" + prev + ".";
            case COMPLETED -> "Booking has been completed.";
            case CANCELLED -> "Booking has been cancelled" + prev + ".";
            case TICKET_CANCELLED -> "Ticket has been cancelled" + prev + ".";
            case VOID -> "Booking has been voided" + prev + ".";
            case REFUND -> "Refund has been processed" + prev + ".";
            case REJECTED -> "Booking has been rejected" + prev + outcome + ".";
            case FAILED -> "Booking process failed" + prev + outcome + ".";
            case REPRICE -> "Booking price has been updated" + prev + ".";
            case VALIDATION_PROCESS -> "Validating flight price with the airline.";
            case VALIDATION_SUCCESS -> "Price validation succeeded.";
            case VALIDATION_FAILED -> "Price validation failed" + outcome + ".";
            case VALIDATION_PRICE_CHANGED -> "Price has changed during validation.";
            case SEARCH -> "User completed a flight search.";
            case SEARCH_FAILED -> "Flight search failed" + outcome + ".";
            case BUNDLE_VALIDATION_SUCCESS -> "Bundle / baggage price validation succeeded.";
            case BUNDLE_VALIDATION_FAILED -> "Bundle / baggage price validation failed" + outcome + ".";
            case ADD_TO_CART -> "Flight added to cart (FlyDubai).";
            case ADD_TO_CART_FAILED -> "Add to cart failed" + outcome + ".";
            default -> "Booking status changed to " + status.name() + prev + outcome + ".";
        };
    }
}
