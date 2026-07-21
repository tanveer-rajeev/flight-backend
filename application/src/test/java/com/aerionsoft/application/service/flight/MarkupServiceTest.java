package com.aerionsoft.application.service.flight;

import com.aerionsoft.application.dto.flight.MarkupCombinedCondition;
import com.aerionsoft.application.dto.flight.search.Response;
import com.aerionsoft.application.dto.flight.search.extras.Airline;
import com.aerionsoft.application.dto.flight.search.extras.Airport;
import com.aerionsoft.application.dto.flight.search.extras.FinalFare;
import com.aerionsoft.application.dto.flight.search.extras.Location;
import com.aerionsoft.application.dto.flight.search.extras.Segments;
import com.aerionsoft.application.entity.MarkupRule;
import com.aerionsoft.application.enums.flight.MarkupFilterMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkupServiceTest {

    private final MarkupService markupService = new MarkupService();

    @Test
    void combinedRuleAppliesToOneWayWhenTheSegmentMatches() {
        Response oneWay = response(segment(1, "DXB", "DAC", "BS", "S"));
        MarkupRule combined = combinedRule("DXB-DAC", "BS", "S", "E", "K", "Q");

        assertTrue(markupService.isRuleApplicable(combined, oneWay, fare()));
    }

    @Test
    void combinedRuleNeverAppliesToRoundTrip() {
        Response roundTrip = roundTripResponse(
                segment(1, "DXB", "DAC", "BS", "S"),
                segment(2, "DAC", "DXB", "BS", "S")
        );
        // Both directions listed, yet a class-combination rule must not apply to a round trip.
        MarkupRule combined = combinedRule("DXB-DAC", "BS", "S", "E", "K", "Q");
        MarkupRule combinedBothWays = combinedRuleFrom(
                new MarkupCombinedCondition("DXB-DAC", "BS", "S"),
                new MarkupCombinedCondition("DAC-DXB", "BS", "S")
        );
        MarkupRule routeRule = individualRouteRule("DXB-DAC,DXB-CGP,SHJ-DAC,SHJ-CGP,AUH-DAC,AUH-CGP");

        assertFalse(markupService.isRuleApplicable(combined, roundTrip, fare()));
        assertFalse(markupService.isRuleApplicable(combinedBothWays, roundTrip, fare()));
        // The route rule matches the outbound leg and should apply instead (e.g. 11.5%).
        assertTrue(markupService.isRuleApplicable(routeRule, roundTrip, fare()));
    }

    @Test
    void combinedRuleDoesNotApplyWhenItineraryReturnsToOriginWithoutIsReturnFlag() {
        Response roundTrip = response(
                segment(1, "DXB", "DAC", "BS", "S"),
                segment(2, "DAC", "DXB", "BS", "S")
        );
        MarkupRule combined = combinedRule("DXB-DAC", "BS", "S", "E", "K", "Q");

        assertFalse(markupService.isRuleApplicable(combined, roundTrip, fare()));
    }

    @Test
    void repriceToUnlistedBookingCodeDropsCombinedRule() {
        Response oneWay = response(segment(1, "DXB", "DAC", "BS", "S"));
        MarkupRule combined = combinedRule("DXB-DAC", "BS", "S", "E", "K", "Q");
        MarkupRule routeRule = individualRouteRule("DXB-DAC");
        List<MarkupService.MarkupSegmentSelection> repriceToB =
                List.of(new MarkupService.MarkupSegmentSelection(1, "B"));

        assertFalse(markupService.isRuleApplicable(combined, oneWay, fare(), repriceToB));
        assertTrue(markupService.isRuleApplicable(routeRule, oneWay, fare(), repriceToB));
    }

    private static MarkupRule individualRouteRule(String routes) {
        MarkupRule rule = baseRule();
        rule.setFilterMode(MarkupFilterMode.INDIVIDUAL);
        rule.setRoutes(routes);
        return rule;
    }

    private static MarkupRule combinedRule(String route, String airlineCode, String... bookingCodes) {
        MarkupRule rule = baseRule();
        rule.setFilterMode(MarkupFilterMode.COMBINED);
        rule.setCombinedConditions(
                java.util.Arrays.stream(bookingCodes)
                        .map(code -> new MarkupCombinedCondition(route, airlineCode, code))
                        .toList());
        return rule;
    }

    private static MarkupRule combinedRuleFrom(MarkupCombinedCondition... conditions) {
        MarkupRule rule = baseRule();
        rule.setFilterMode(MarkupFilterMode.COMBINED);
        rule.setCombinedConditions(List.of(conditions));
        return rule;
    }

    private static MarkupRule baseRule() {
        MarkupRule rule = new MarkupRule();
        rule.setProvider("Any");
        rule.setOrigin("Any");
        rule.setAppliedOn("Always");
        return rule;
    }

    private static Response response(Segments... segments) {
        Response response = new Response();
        response.setChannel("USBANGLAAPI");
        response.setSegments(List.of(segments));
        return response;
    }

    private static Response roundTripResponse(Segments... segments) {
        Response response = response(segments);
        response.setIsReturn(true);
        return response;
    }

    private static Segments segment(int leg, String origin, String destination, String airlineCode, String bookingCode) {
        Segments segment = new Segments();
        segment.setLeg(leg);
        segment.setOrigin(location(origin));
        segment.setDestination(location(destination));
        segment.setAirline(airline(airlineCode));
        segment.setBookingCode(bookingCode);
        return segment;
    }

    private static Location location(String airportCode) {
        Airport airport = new Airport();
        airport.setAirportCode(airportCode);
        airport.setCountryCode("BD");

        Location location = new Location();
        location.setAirport(airport);
        return location;
    }

    private static Airline airline(String airlineCode) {
        Airline airline = new Airline();
        airline.setAirlineCode(airlineCode);
        return airline;
    }

    private static FinalFare fare() {
        FinalFare fare = new FinalFare();
        fare.setBaseFare(100.0);
        return fare;
    }
}
