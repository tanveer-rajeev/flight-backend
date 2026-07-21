package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.GroupTicket.GroupTicketDTO;
import com.aerionsoft.application.dto.common.PaginationResponseDto;
import com.aerionsoft.application.scheduler.GroupTicketExpiryScheduler;
import com.aerionsoft.application.service.admin.GroupTicketService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@Validated
@RequestMapping("/api/admin/group-tickets")
public class GroupTicketController {

    @Autowired
    private GroupTicketService groupTicketService;

    @Autowired
    private GroupTicketExpiryScheduler groupTicketExpiryScheduler;

    @PostMapping
    public ResponseEntity<BaseResponse<GroupTicketDTO>> create(@Valid @RequestBody GroupTicketDTO groupTicketDTO) {
        GroupTicketDTO created = groupTicketService.createGroupTicket(groupTicketDTO);
        return ResponseEntity.ok(BaseResponse.ok(created));
    }

    @PutMapping("/{gfCode}")
    public ResponseEntity<BaseResponse<GroupTicketDTO>> update(
            @PathVariable String gfCode,
            @Valid @RequestBody GroupTicketDTO groupTicketDTO) {
        GroupTicketDTO updated = groupTicketService.updateGroupTicket(gfCode, groupTicketDTO);
        return ResponseEntity.ok(BaseResponse.ok(updated));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PaginationResponseDto<GroupTicketDTO>>> list(
            @RequestParam(required = false) String airlineCode,
            @RequestParam(required = false) String pnr,
            @RequestParam(required = false) String ticketType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginationResponseDto<GroupTicketDTO> tickets = groupTicketService.listGroupTickets(airlineCode, status, pnr, ticketType, agencyId, page, size);
        return ResponseEntity.ok(BaseResponse.ok(tickets));
    }

    @GetMapping("/{gfCode}")
    public ResponseEntity<BaseResponse<GroupTicketDTO>> get(@PathVariable String gfCode) {
        GroupTicketDTO ticket = groupTicketService.getGroupTicket(gfCode);
        return ResponseEntity.ok(BaseResponse.ok(ticket));
    }

    @DeleteMapping("/{gfCode}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String gfCode) {
        groupTicketService.deleteGroupTicket(gfCode);
        return ResponseEntity.ok(BaseResponse.ok(null));
    }

    /**
     * Manually trigger the group-ticket auto-expiry job.
     * Useful for testing or on-demand runs without waiting for midnight.
     *
     * POST /api/admin/group-tickets/trigger-expiry
     */
    @PostMapping("/trigger-expiry")
    public ResponseEntity<BaseResponse<String>> triggerExpiry() {
        String result = groupTicketExpiryScheduler.triggerManualExpiry();
        return ResponseEntity.ok(BaseResponse.ok(result));
    }
}

