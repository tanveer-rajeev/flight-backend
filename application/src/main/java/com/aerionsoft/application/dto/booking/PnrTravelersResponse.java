package com.aerionsoft.application.dto.booking;

import com.aerionsoft.application.dto.admin.GroupTicket.GroupTicketDTO;
import com.aerionsoft.application.dto.agency.AgencyInfo;
import com.aerionsoft.application.dto.traveller.TravellerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PnrTravelersResponse {
    private String gfCode;
    private String pnr;
    private String airlinePnr;
    private String origin;
    private String destination;
    private String airlineCode;
    private String airlineName;
    private List<BookingWithTravelers> bookings;
    private GroupTicketDTO groupTicket;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingWithTravelers {
        private Long bookingId;
        private String bookingReference;
        private String status;
        private String bookingDate;
        private List<TravellerResponse> travelers;
        private List<SegmentDTO> segments;
        private AgencyInfo agencyInfo;
    }
}
