package com.aerionsoft.application.controller.booking;

import com.aerionsoft.application.annotation.SkipAutoAudit;
import com.aerionsoft.application.controller.BaseController;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.ticketaction.AdminTicketActionFinalizeRequest;
import com.aerionsoft.application.dto.ticketaction.AdminTicketActionQuoteRequest;
import com.aerionsoft.application.dto.ticketaction.AdminTicketActionRejectRequest;
import com.aerionsoft.application.dto.ticketaction.TicketActionRequestResponse;
import com.aerionsoft.application.service.booking.TicketActionRequestService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@SkipAutoAudit
@RequestMapping("/api/admin/ticket-actions")
public class TicketActionRequestAdminController extends BaseController {

    private final TicketActionRequestService ticketActionRequestService;

    public TicketActionRequestAdminController(TicketActionRequestService ticketActionRequestService) {
        this.ticketActionRequestService = ticketActionRequestService;
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-view-ticket-action-requests')")
    public ResponseEntity<BaseResponse<Page<TicketActionRequestResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<TicketActionRequestResponse> result = ticketActionRequestService.adminList(status, type, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Ticket action requests fetched successfully", result));
    }

    @GetMapping("/confirmed")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-view-ticket-action-requests')")
    public ResponseEntity<BaseResponse<Page<TicketActionRequestResponse>>> listConfirmed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<TicketActionRequestResponse> result = ticketActionRequestService.adminList("USER_CONFIRMED", null, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Confirmed ticket action requests fetched successfully", result));
    }

    @PostMapping("/{requestId}/quote")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-quote-ticket-action-request')")
    public ResponseEntity<BaseResponse<TicketActionRequestResponse>> quote(
            @PathVariable Long requestId,
            @Valid @RequestBody AdminTicketActionQuoteRequest request
    ) {
        Long adminUserId = getUserIdFromAuthentication();
        TicketActionRequestResponse res = ticketActionRequestService.adminQuote(requestId, request, adminUserId);
        return ResponseEntity.ok(BaseResponse.ok("Quote sent successfully", res));
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-quote-ticket-action-request')")
    public ResponseEntity<BaseResponse<TicketActionRequestResponse>> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody AdminTicketActionRejectRequest request
    ) {
        Long adminUserId = getUserIdFromAuthentication();
        TicketActionRequestResponse res = ticketActionRequestService.adminReject(requestId, request, adminUserId);
        return ResponseEntity.ok(BaseResponse.ok("Request rejected", res));
    }

    @PostMapping("/{requestId}/start-processing")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-finalize-ticket-action-request')")
    public ResponseEntity<BaseResponse<TicketActionRequestResponse>> startProcessing(
            @PathVariable Long requestId
    ) {
        Long adminUserId = getUserIdFromAuthentication();
        TicketActionRequestResponse res = ticketActionRequestService.adminStartProcessing(requestId, adminUserId);
        return ResponseEntity.ok(BaseResponse.ok("Request moved to processing", res));
    }

    @PostMapping("/{requestId}/finalize")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-finalize-ticket-action-request')")
    public ResponseEntity<BaseResponse<TicketActionRequestResponse>> finalizeRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody AdminTicketActionFinalizeRequest request
    ) {
        Long adminUserId = getUserIdFromAuthentication();
        TicketActionRequestResponse res = ticketActionRequestService.adminFinalize(requestId, request, adminUserId);
        return ResponseEntity.ok(BaseResponse.ok("Request finalized", res));
    }
}
