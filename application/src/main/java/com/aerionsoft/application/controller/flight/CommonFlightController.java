package com.aerionsoft.application.controller.flight;

import org.springframework.validation.annotation.Validated;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.GroupTicket.GroupTicketDTO;
import com.aerionsoft.application.entity.Currency;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.enums.group.GroupTicketType;
import com.aerionsoft.application.service.booking.BookingService;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.service.admin.GroupTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/flight/common")
public class CommonFlightController {

    @Autowired
    private GroupTicketService groupTicketService;
    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/group-tickets")
    public ResponseEntity<BaseResponse<List<GroupTicketDTO>>> hello(
            @RequestParam String departureDate,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String returnDate
    ){

        List<GroupTicketDTO> tickets = groupTicketService.getAvailableTickets(departureDate, origin, destination, returnDate);
        return ResponseEntity.ok(BaseResponse.ok(tickets));
    }

    /**
     * Endpoint to list all currencies
     *
     * @return ResponseEntity with BaseResponse containing list of currencies
     */
    @GetMapping("/currencies")
    public ResponseEntity<BaseResponse<List<Currency>>> list() {
        return ResponseEntity.ok(BaseResponse.ok(currencyService.listAll()));
    }


    @GetMapping("/common-currencies")
    public ResponseEntity<BaseResponse<List<Currency>>> commonCur(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String channel) {
        if (provider != null && !provider.isBlank() && Provider.getByName(provider) == null) {
            return ResponseEntity.badRequest().body(BaseResponse.error("Invalid provider name"));
        }
        return ResponseEntity.ok(BaseResponse.ok(currencyService.listCommonCurrencies(provider, channel)));
    }



    @GetMapping("/group-special-fares")
    public ResponseEntity<BaseResponse<List<GroupTicketDTO>>> getGroupSpecialFares() {
        List<GroupTicketDTO> tickets = groupTicketService.getGroupSpecialFares(GroupTicketType.GROUP);
        return ResponseEntity.ok(BaseResponse.ok(tickets));
    }

    @GetMapping("/group-umrah-fares")
    public ResponseEntity<BaseResponse<List<GroupTicketDTO>>> getGroupUmrahFares() {
        List<GroupTicketDTO> tickets = groupTicketService.getGroupSpecialFares(GroupTicketType.UMRAH);
        return ResponseEntity.ok(BaseResponse.ok(tickets));
    }

    @GetMapping("/group-a2a-fares")
    public ResponseEntity<BaseResponse<List<GroupTicketDTO>>> getGroupA2aFares() {
        List<GroupTicketDTO> tickets = groupTicketService.getGroupSpecialFares(GroupTicketType.A2A);
        return ResponseEntity.ok(BaseResponse.ok(tickets));
    }


}

