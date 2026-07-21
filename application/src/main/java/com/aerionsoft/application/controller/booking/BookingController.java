package com.aerionsoft.application.controller.booking;

import com.aerionsoft.application.dto.booking.*;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.annotation.SkipAutoAudit;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.flight.GetReservationRequest;
import com.aerionsoft.application.dto.flight.GetReservationResponse;
import com.aerionsoft.application.dto.flight.LoadBookingRequest;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.service.booking.BookingCoordinatorService;
import com.aerionsoft.application.service.booking.BookingService;
import com.aerionsoft.application.service.user.UserService;
import com.aerionsoft.application.scheduler.TicketingDeadlineScheduler;
import com.aerionsoft.application.service.booking.TicketingDeadlineService;
import com.aerionsoft.application.dto.ticketaction.TicketActionRequestCreateRequest;
import com.aerionsoft.application.dto.ticketaction.TicketActionRequestResponse;
import com.aerionsoft.application.service.booking.TicketActionRequestService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.netty.http.server.HttpServerRequest;

import java.time.LocalDate;

@SkipAutoAudit
@RestController
@Validated
@RequestMapping("/api/bookings")
public class BookingController extends BaseController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingCoordinatorService bookingCoordinatorService;

    @Autowired
    private TicketingDeadlineService ticketingDeadlineService;

    @Autowired
    private TicketingDeadlineScheduler ticketingDeadlineScheduler;

    @Autowired
    private TicketActionRequestService ticketActionRequestService;

    @Autowired
    private BookingRepository bookingRepository;


    @PostMapping("/create")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-booking')") // admin and user
    public ResponseEntity<BaseResponse<BookingResponse>> create(
            @Valid @RequestBody BookingRequest req,
            HttpServerRequest request
    ) {
        Long currentUserId = getUserIdFromAuthentication();

        boolean bypassBilling = bypassBillingIfVlife();

        if (req.getBookType() == null) {
            return ResponseEntity.badRequest().body(BaseResponse.error("Book type is required"));
        }

        // Determine if this is a child user acting for parent
        Long targetUserId = currentUserId;
        Long actingUserId = null;

        if (!isAdmin()) {
            // Check if current user has a parent
            var currentUser = userService.getUserById(currentUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                // Child user - booking goes to parent's account
                targetUserId = currentUser.getParentUserId();
                actingUserId = currentUserId;
            }
        }

        return ResponseEntity.ok(BaseResponse.ok("Booking created successfully", bookingCoordinatorService.create(req, targetUserId, actingUserId, bypassBilling)));
    }

    /**
     * Create a manual booking with direct ticket/PNR information
     * Used for offline bookings where ticket details are entered manually
     */

    @PostMapping("/create-manual")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-booking-manual')")
    public ResponseEntity<BaseResponse<BookingResponse>> createManualBooking(@Valid @RequestBody ManualBookingRequest req) {
        Long currentUserId = getUserIdFromAuthentication();


        return ResponseEntity.ok(BaseResponse.ok("Manual booking created successfully",
                bookingCoordinatorService.createManualBooking(req, currentUserId)));
    }

    @PostMapping("/import-pnr")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-booking-manual')")
    public ResponseEntity<BaseResponse<BookingResponse>> importPnr(@Valid @RequestBody ImportPnrRequest req) {
        Long currentUserId = getUserIdFromAuthentication();

        return ResponseEntity.ok(BaseResponse.ok("PNR imported successfully",
                bookingCoordinatorService.importConfirmedPnr(req, currentUserId, isAdmin())));
    }

    /**
     * Create a manual booking from a group ticket.
     * Segment information is automatically fetched from the GroupTicket entity's flight info.
     * Works similarly to import-pnr but derives segments, PNR, and airline info from the GroupTicket.
     */
    @PostMapping("/group-ticket-manual")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-booking-manual')")
    public ResponseEntity<BaseResponse<BookingResponse>> groupTicketManualBooking(
            @Valid @RequestBody GroupTicketManualRequest req) {
        Long currentUserId = getUserIdFromAuthentication();

        return ResponseEntity.ok(BaseResponse.ok("Group ticket manual booking created successfully",
                bookingCoordinatorService.groupTicketManualBooking(req, currentUserId, isAdmin())));
    }


    @PostMapping("/issue-ticket")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-issue-ticket')") // admin and user
    public ResponseEntity<BaseResponse<String>> issueTicket(@Valid @RequestParam Long bookingId, @Valid @RequestParam String salt) {
        Long userId = getUserIdFromAuthentication();
        String correctedSalt = salt.replace(" ", "+");

        return ResponseEntity.ok(BaseResponse.ok(bookingCoordinatorService.issue(bookingId, userId, correctedSalt)));
    }


    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-booking')") // admin and user
    public ResponseEntity<BaseResponse<BookingResponse>> update(@PathVariable Long id, @Valid @RequestBody BookingRequest req) {
        return ResponseEntity.ok(BaseResponse.ok("Booking updated successfully", bookingService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-booking')") // admin and user
    public ResponseEntity<BaseResponse<BookingDeleteResponse>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok("Booking deleted successfully", bookingService.delete(id)));
    }

    @GetMapping("/list")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')") // admin and user
    public ResponseEntity<BaseResponse<Page<BookingResponse>>> list(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String pnrOrId,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return listBySourceType(currency, fromDate, toDate, pnrOrId, ticketNo, status, page, size, authentication, "ONLINE", "Bookings fetched successfully");
    }

    @GetMapping("/list/all")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')") // admin and user
    public ResponseEntity<BaseResponse<Page<BookingResponse>>> listAll(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String pnrOrId,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return listBySourceType(currency, fromDate, toDate, pnrOrId, ticketNo, status, page, size, authentication, null, "All bookings fetched successfully");
    }

    @GetMapping("/list/manual")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')") // admin and user
    public ResponseEntity<BaseResponse<Page<BookingResponse>>> listManual(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String pnrOrId,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return listBySourceType(currency, fromDate, toDate, pnrOrId, ticketNo, status, page, size, authentication, "MANUAL", "Manual bookings fetched successfully");
    }

    @GetMapping("/list/import")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')") // admin and user
    public ResponseEntity<BaseResponse<Page<BookingResponse>>> listImport(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String pnrOrId,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return listBySourceType(currency, fromDate, toDate, pnrOrId, ticketNo, status, page, size, authentication, "IMPORT", "Imported bookings fetched successfully");
    }

    @GetMapping("/list/group")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')") // admin and user
    public ResponseEntity<BaseResponse<Page<BookingResponse>>> listGroup(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String pnrOrId,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return listBySourceType(currency, fromDate, toDate, pnrOrId, ticketNo, status, page, size, authentication, "GROUP", "Group bookings fetched successfully");
    }

    private ResponseEntity<BaseResponse<Page<BookingResponse>>> listBySourceType(
            String currency,
            LocalDate fromDate,
            LocalDate toDate,
            String pnrOrId,
            String ticketNo,
            String status,
            int page,
            int size,
            Authentication authentication,
            String sourceType,
            String successMessage
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        BookingStatus bookingStatus = null;

        if (status != null && !status.isEmpty()) {
            bookingStatus = java.util.Arrays.stream(BookingStatus.values())
                    .filter(s -> s.name().equalsIgnoreCase(status))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid booking status: " + status));
        }

        Long targetUserId = authUserId;
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (!isAdmin) {
            var currentUser = userService.getUserById(authUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                targetUserId = currentUser.getParentUserId();
            }
        }

        if (isAdmin && currency != null && !currency.isBlank()) {
            java.util.Arrays.stream(Currency.values())
                    .filter(c -> c.name().equalsIgnoreCase(currency))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid currency: " + currency));
        }

        String currencyFilter = isAdmin ? currency : null;

        Page<BookingResponse> bookings = bookingService.search(
                currencyFilter, fromDate, toDate, pnrOrId, ticketNo, bookingStatus, page, size, isAdmin, targetUserId, sourceType);
        return ResponseEntity.ok(BaseResponse.ok(successMessage, bookings));
    }

    @GetMapping("/list/user")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')") // admin and user
    public ResponseEntity<BaseResponse<Page<BookingResponse>>> listByUser(
            @RequestParam(required = false) String currency,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String pnrOrId,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        BookingStatus bookingStatus = null;

        // Handle string to enum conversion
        if (status != null && !status.isEmpty()) {
            bookingStatus = java.util.Arrays.stream(BookingStatus.values())
                    .filter(s -> s.name().equalsIgnoreCase(status))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid booking status: " + status));
        }

        // For non-admin users, force the search to be limited to the specific user
        Page<BookingResponse> bookings = bookingService.search(null, fromDate, toDate, pnrOrId, ticketNo, bookingStatus, page, size, false, userId, null);
        return ResponseEntity.ok(BaseResponse.ok("Bookings fetched successfully for user " + userId, bookings));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')") // admin and user
    public ResponseEntity<BaseResponse<BookingResponse>> getById(@PathVariable Long id) {
        Long currentUserId = getUserIdFromAuthentication();

        // Determine if this is a child user
        Long targetUserId = currentUserId;
        if (!isAdmin()) {
            var currentUser = userService.getUserById(currentUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                // Child user - can view parent's bookings
                targetUserId = currentUser.getParentUserId();
            }
        }

        BookingResponse booking = bookingService.getById(id, targetUserId, isAdmin());


        return ResponseEntity.ok(BaseResponse.ok("Booking fetched successfully", booking));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-booking')") // admin and user
    public ResponseEntity<BaseResponse<BookingResponse>> changeStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {

        BookingStatus bookingStatus = java.util.Arrays.stream(BookingStatus.values())
                .filter(s -> s.name().equalsIgnoreCase(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid booking status: " + status));

        BookingResponse booking = bookingService.changeStatus(id, bookingStatus, reason);
        return ResponseEntity.ok(BaseResponse.ok("Booking status updated successfully", booking));
    }

    /**
     * Admin-only endpoint to issue a full or partial refund for a booking.
     *
     * <ul>
     *   <li><b>FULL</b>  – the entire booking price is credited back to the user's wallet.</li>
     *   <li><b>PARTIAL</b> – supply {@code deductionAmount} (the penalty/fee to keep from the customer);
     *       the remainder ({@code bookingPrice - deductionAmount}) is credited to the wallet.</li>
     *   <li><b>supplierRefundCost</b> – amount the supplier keeps (remaining payable for the PNR).
     *       Supplier payable reversal = {@code buyPrice - supplierRefundCost}. Use {@code 0} when the
     *       supplier credits the full buy price.</li>
     * </ul>
     *
     * <pre>
     * POST /api/bookings/{id}/admin/refund
     * {
     *   "refundType"          : "FULL" | "PARTIAL",
     *   "deductionAmount"     : 50.00,          // required only for PARTIAL (customer-side fee)
     *   "supplierRefundCost"  : 200.00,         // required; supplier cost kept on refund (USD)
     *   "reason"              : "Cancellation penalty applied"
     * }
     * </pre>
     */
    @PostMapping("/{id}/admin/refund")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-booking-admin')")
    public ResponseEntity<BaseResponse<AdminBookingRefundResponse>> adminRefundBooking(
            @PathVariable Long id,
            @Valid @RequestBody AdminBookingRefundRequest request) {

        AdminBookingRefundResponse response = bookingService.adminRefundBooking(id, request);
        return ResponseEntity.ok(BaseResponse.ok(
                request.getRefundType().name() + " refund processed successfully for booking " + id,
                response));
    }

    /**
     * Admin edit: PNR, sell/buy price, pax names, and optional agency transfer.
     * Append-only wallet/supplier adjustments — does not delete PURCHASE rows.
     *
     * <pre>
     * PATCH /api/bookings/{id}/admin/edit
     * {
     *   "reason": "Corrected PNR",
     *   "pnr": "XYZ789",
     *   "bookingPrice": 1300.00,   // agency currency (target agency if transferring)
     *   "buyPrice": 850.00,        // agency currency (target agency if transferring)
     *   "travellers": [{ "travellerId": 55, "firstName": "Jon", "lastName": "Doh" }],
     *   "targetUserId": 42
     * }
     * </pre>
     */
    @PutMapping("/{id}/admin/edit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-booking-admin')")
    public ResponseEntity<BaseResponse<AdminBookingEditResponse>> adminEditBooking(
            @PathVariable Long id,
            @Valid @RequestBody AdminBookingEditRequest request) {

        AdminBookingEditResponse response = bookingService.adminEditBooking(id, request);
        return ResponseEntity.ok(BaseResponse.ok(
                "Booking " + id + " updated successfully", response));
    }

    /**
     * Admin-only endpoint to change a booking status with a mandatory reason.
     * Uses a JSON request body so the reason is clearly documented and required.
     */
    @PutMapping("/{id}/admin/status")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-booking-admin')")
    public ResponseEntity<BaseResponse<BookingResponse>> adminChangeStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminBookingStatusChangeRequest request) {

        BookingResponse booking = bookingService.changeStatus(id, request.getStatus(), request.getReason());
        return ResponseEntity.ok(BaseResponse.ok(
                "Booking status updated to " + request.getStatus() + " successfully", booking));
    }

    @PutMapping("/{id}/status-ticket")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-booking')") // admin and user
    public ResponseEntity<BaseResponse<String>> updateStatusAndTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {

        boolean isAdmin = isAdmin();
        Long currentUserId = getUserIdFromAuthentication();
        bookingService.updateBookingStatus(id, request.getStatus(), request.getReason(), request.getTicketNumber(), isAdmin, currentUserId, null);
        return ResponseEntity.ok(BaseResponse.ok("Booking status and ticket number updated successfully"));
    }

    @PostMapping("/hold-to-book")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'hold-booking')") // admin and user
    public ResponseEntity<BaseResponse<RepricedResponse>> holdToBook(@Valid @RequestBody HoldToBook book, @Valid @RequestParam String salt) {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        RepricedResponse repricedResponse = bookingCoordinatorService.holdToBook(book.getBookingId(), userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(repricedResponse));
    }


    @PostMapping(value = "/cancel-booking", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@permissionService.hasPermission(authentication, 'cancel-booking')") // admin and user
    public ResponseEntity<BaseResponse<Boolean>> cancelBooking(
            @Valid @RequestBody CancelBookingRequest request
    ) {
        // Currently we treat confirmationId as Booking.pnr
        Boolean updated = bookingCoordinatorService.cancelByPnr(
                request.getConfirmationId(),
                request.getChannel(), request.getReason()
        );

        return ResponseEntity.ok(BaseResponse.ok("Booking cancelled successfully", updated));
    }

    /**
     * Manual endpoint to update ticketing deadline for a specific Sabre booking
     * Useful for testing or manual intervention
     */
    @PostMapping("/{id}/update-ticketing-deadline")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-booking')") // admin and user
    public ResponseEntity<BaseResponse<Boolean>> updateTicketingDeadline(@PathVariable Long id) {
        boolean success = ticketingDeadlineService.manualUpdateTicketingDeadline(id);

        if (success) {
            return ResponseEntity.ok(BaseResponse.ok("Ticketing deadline updated successfully", true));
        } else {
            return ResponseEntity.badRequest().body(BaseResponse.error(400, "Failed to update ticketing deadline"));
        }
    }

    /**
     * Manual trigger for the scheduled job to update all Sabre + Verteil bookings without ticketing deadline
     * This will update previous bookings that may have missed the automatic update
     * Admin only
     */
    @PostMapping("/trigger-deadline-update")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<BaseResponse<String>> triggerDeadlineUpdate() {
        String result = ticketingDeadlineScheduler.triggerManualUpdate();
        return ResponseEntity.ok(BaseResponse.ok(result));
    }

    /**
     * Get count of Sabre + Verteil bookings that need ticketing deadline update
     * Useful for monitoring dashboard
     */
    @GetMapping("/bookings-needing-deadline-update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Long>> getBookingsNeedingUpdate() {
        long count = ticketingDeadlineScheduler.getCountOfBookingsNeedingUpdate();
        return ResponseEntity.ok(BaseResponse.ok(
                count > 0
                        ? count + " booking(s) need ticketing deadline update"
                        : "All bookings have ticketing deadline set",
                count
        ));
    }

    @PostMapping("/{bookingId}/ticket-actions")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-ticket-action-request')") // admin and user
    public ResponseEntity<BaseResponse<TicketActionRequestResponse>> createTicketActionRequest(
            @PathVariable Long bookingId,
            @Valid @RequestBody TicketActionRequestCreateRequest request
    ) {
        Long currentUserId = getUserIdFromAuthentication();
        TicketActionRequestResponse res = ticketActionRequestService.create(bookingId, request, currentUserId, isAdmin());
        return ResponseEntity.ok(BaseResponse.ok("Ticket action request created", res));
    }

    @GetMapping("/{bookingId}/ticket-actions")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-ticket-action-request')") // admin and user
    public ResponseEntity<BaseResponse<Page<TicketActionRequestResponse>>> listTicketActionRequests(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentUserId = getUserIdFromAuthentication();
        Page<TicketActionRequestResponse> res = ticketActionRequestService.listForBooking(bookingId, page, size, currentUserId, isAdmin());
        return ResponseEntity.ok(BaseResponse.ok("Ticket action requests fetched", res));
    }

    @GetMapping("/{bookingId}/ticket-actions/{requestId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-ticket-action-request')") // admin and user
    public ResponseEntity<BaseResponse<TicketActionRequestResponse>> getTicketActionRequest(
            @PathVariable Long bookingId,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getUserIdFromAuthentication();
        TicketActionRequestResponse res = ticketActionRequestService.getForBooking(bookingId, requestId, currentUserId, isAdmin());
        return ResponseEntity.ok(BaseResponse.ok("Ticket action request fetched", res));
    }

    @PostMapping("/{bookingId}/ticket-actions/{requestId}/confirm")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'confirm-ticket-action-quote')") // admin and user
    public ResponseEntity<BaseResponse<TicketActionRequestResponse>> confirmTicketActionQuote(
            @PathVariable Long bookingId,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getUserIdFromAuthentication();
        TicketActionRequestResponse res = ticketActionRequestService.userConfirm(bookingId, requestId, currentUserId, isAdmin());
        return ResponseEntity.ok(BaseResponse.ok("Ticket action quote confirmed", res));
    }

    /**
     * Search bookings and travelers by GF code
     * Returns PNR information and all associated bookings with travelers
     */
    @GetMapping("/search-by-gf-code")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')") // admin and user
    public ResponseEntity<BaseResponse<PnrTravelersResponse>> searchByGfCode(
            @RequestParam String gfCode
    ) {
        PnrTravelersResponse response = bookingService.findBookingsByGfCode(gfCode);
        return ResponseEntity.ok(BaseResponse.ok("Bookings and travelers found for GF code: " + gfCode, response));
    }


    @PostMapping("reprice-confirmation")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'hold-booking')") // admin and user
    public ResponseEntity<BaseResponse<Boolean>> repriceConfirmation(@Valid @RequestBody RepriceConfirmationRequest request) {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        boolean repricedResponse = bookingCoordinatorService.repriceConfirmation(request, userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(repricedResponse));
    }

    /**
     * Get the full timeline (status history) of a booking.
     */
    @GetMapping("/{id}/timeline")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')")
    public ResponseEntity<BaseResponse<List<BookingTimelineDTO>>> getBookingTimeline(@PathVariable Long id) {
        List<BookingTimelineDTO> timeline = bookingService.getBookingTimeline(id);
        return ResponseEntity.ok(BaseResponse.ok("Booking timeline fetched successfully", timeline));
    }

    /**
     * Pre-booking journey for a search session (search → validation → bundle → cart).
     * GET /api/bookings/flight-activity/session?sessionId=xxx
     */
    @GetMapping("/flight-activity/session")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-booking')")
    public ResponseEntity<BaseResponse<List<BookingTimelineDTO>>> getFlightActivityBySession(
            @RequestParam String sessionId) {
        List<BookingTimelineDTO> timeline = bookingService.getFlightActivityBySession(sessionId);
        return ResponseEntity.ok(BaseResponse.ok("Flight activity timeline fetched", timeline));
    }

    @PostMapping(value = "/get-reservation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<GetReservationResponse>> getReservation(
            @RequestParam(required = false) String sessionId,
            @Valid @RequestBody GetReservationRequest request
    ) {
        if (bookingRepository.existsByPnrIgnoreCase(request.getPnr())) {
            return ResponseEntity.ok(BaseResponse.error("PNR " + request.getPnr() + " already exists in the system"));
        }

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        BaseResponse<GetReservationResponse> response = bookingCoordinatorService.getReservation(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/load-booking", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<Map<String, Object>>> loadBooking(
            @RequestParam(required = false) String sessionId,
            @Valid @RequestBody LoadBookingRequest request
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        BaseResponse<Map<String, Object>> response = bookingCoordinatorService.loadBooking(sessionId, request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/manual-status-change")
    public ResponseEntity<BaseResponse<Boolean>> manualStatusChange(
            @RequestParam Long bookingId,
            @RequestParam BookingStatus newStatus,
            @RequestParam String reason
    ) {
        boolean success = bookingService.manualStatusChange(bookingId, newStatus, reason);
        if (success) {
            return ResponseEntity.ok(BaseResponse.ok("Booking status changed successfully", true));
        } else {
            return ResponseEntity.badRequest().body(BaseResponse.error(400, "Failed to change booking status"));
        }
    }


}


