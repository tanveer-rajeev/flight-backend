package com.aerionsoft.application.controller.flight;


import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.GroupTicket.GroupTicketDTO;
import com.aerionsoft.application.dto.admin.GroupTicket.Records;
import com.aerionsoft.application.dto.booking.BookingResponse;
import com.aerionsoft.application.dto.flight.StatusChangeRequest;
import com.aerionsoft.application.service.booking.BookingService;
import com.aerionsoft.application.service.admin.GroupTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/core/callback")
public class CoreCallBackController {
    @Autowired
    private BookingService bookingService;
    @Autowired
    GroupTicketService groupTicketService;

    @PostMapping("booking/status-change")
    public ResponseEntity<BaseResponse<BookingResponse>> changeStatus(
            @RequestParam Long id,
            @Valid @RequestBody StatusChangeRequest status
    ) {
        bookingService.changeStatus(id, status);
        return ResponseEntity.ok(BaseResponse.ok("Booking status updated successfully to :: " + status));
    }

    @PostMapping("booking/adjust")
    public ResponseEntity<BaseResponse<Void>> adjust(
            @RequestParam String gfCode,
            @RequestParam Integer qty,
            @Valid @RequestBody Records records
    ) {
        groupTicketService.adjustBooking(gfCode, qty,records);
        return ResponseEntity.ok(BaseResponse.ok("Booking adjusted successfully to :: " + gfCode));
    }

    @GetMapping("/group-ticket")
    public ResponseEntity<BaseResponse<GroupTicketDTO>> getGroupTicket(
            @RequestParam String gfCode
    ) {
        GroupTicketDTO groupTickets = groupTicketService.getGroupTicket(gfCode);
        return ResponseEntity.ok(BaseResponse.ok("Group tickets retrieved successfully", groupTickets));
    }
}
