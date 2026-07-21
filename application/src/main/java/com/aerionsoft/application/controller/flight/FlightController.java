package com.aerionsoft.application.controller.flight;


import com.aerionsoft.application.dto.flight.arabiaBaggage.BaggageValidationWrapper;
import com.aerionsoft.application.dto.flight.arabiaBaggage.Request;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.flight.RecentSearchResponse;
import com.aerionsoft.application.dto.flight.farerules.FareRulesRequest;
import com.aerionsoft.application.dto.flight.farerules.FareRulesResponse;
import com.aerionsoft.application.dto.flight.flydubai.AddToCartRequest;
import com.aerionsoft.application.dto.flight.flydubai.AddToCartResponse;
import com.aerionsoft.application.dto.flight.galileo.RepriceRequest;
import com.aerionsoft.application.dto.flight.search.Response;
import com.aerionsoft.application.dto.flight.search.v1.SearchRequestV1;
import com.aerionsoft.application.dto.flight.validation.PriceValidationRequest;
import com.aerionsoft.application.dto.flight.validation.PriceValidationResponse;
import com.aerionsoft.application.service.flight.FareRulesService;
import com.aerionsoft.application.service.flight.FlyDubaiService;
import com.aerionsoft.application.service.flight.GalileoRepriceService;
import com.aerionsoft.application.service.flight.PriceValidationService;
import com.aerionsoft.application.service.flight.SSRService;
import com.aerionsoft.application.service.flight.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/flights")
public class FlightController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(FlightController.class);

    @Autowired
    private SearchService searchService;

    @Autowired
    private PriceValidationService priceValidationService;

    @Autowired
    private SSRService ssrService;

    @Autowired
    private FlyDubaiService flyDubaiService;

    @Autowired
    private FareRulesService fareRulesService;

    @Autowired
    private GalileoRepriceService galileoRepriceService;


    @PostMapping(value = "/search", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Response> search(@RequestParam String sessionId, @Valid @RequestBody com.aerionsoft.application.dto.flight.search.Request req, HttpServletRequest request) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        if (req.getOrigin() == null || req.getDestination() == null || req.getDepartureDate() == null) {
            return Flux.empty();
        }
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");


        String finalSessionId = sessionId;
        return searchService.aggregate(finalSessionId, req, ip, userAgent)
//                .doOnNext(result ->
////                        log.info("Streaming result for session {}: {}", finalSessionId, 1)
//                )
                .doOnComplete(() ->
                        log.info("[traceId={}] Completed streaming results for session {}",
                                MDC.get("traceId"), finalSessionId)
                );
    }

    @PostMapping(value = "/v1/search", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Response> searchV1(@RequestParam String sessionId,
                                   @Valid @RequestBody SearchRequestV1 requestV1,
                                   HttpServletRequest request) {

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        // Validate request
        if (requestV1.getOriginDestinations() == null || requestV1.getOriginDestinations().isEmpty()) {
            log.warn("[Session: {}] No origin destinations provided", sessionId);
            return Flux.empty();
        }

        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String finalSessionId = sessionId;

        log.info("[Session: {}] V1 Search request with {} origin-destination pairs",
                finalSessionId, requestV1.getOriginDestinations().size());

        // Pass the v1 request as-is to the backend API
        return searchService.aggregateV1(finalSessionId, requestV1, ip, userAgent)
                .doOnComplete(() ->
                        log.info("[traceId={}] Completed V1 search for session {}",
                                MDC.get("traceId"), finalSessionId)
                );
    }

    /**
     * Price validation endpoint - validates flight price with GDS and applies markup.
     * Stores original price and markup amount in Redis for tracking during booking.
     *
     * @param sessionId Session ID from search
     * @param request   Price validation request with passenger details
     * @return Price validation response with markup applied
     */
    @PostMapping(value = "/price-validation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PriceValidationResponse> priceValidation(
            @RequestParam String sessionId,
            @Valid @RequestBody PriceValidationRequest request) {

        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getResultIndex() == null || request.getResultIndex().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getProviderName() == null || request.getProviderName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[Session: {}] Price validation request for resultIndex: {}, provider: {}",
                sessionId, request.getResultIndex(), request.getProviderName());

        PriceValidationResponse response = priceValidationService.validatePrice(sessionId, request);
        return ResponseEntity.ok(response);
    }


    @PostMapping(value = "/bundle-price-validation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaggageValidationWrapper> baggage(
            @RequestParam String sessionId,
            @Valid @RequestBody Request request) {


        if (request.getResultIndex() == null || request.getResultIndex().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getProviderName() == null || request.getProviderName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[Session: {}] Baggage validation request for resultIndex: {}, channel: {}",
                sessionId, request.getResultIndex(), request.getChannel());

        BaggageValidationWrapper response = ssrService.validateBaggage(request, sessionId);
        return ResponseEntity.ok(response);
    }


    /**
     * Galileo reprice endpoint. Proxies the request to the core
     * {@code /api/galileo/flights/reprice} endpoint, applies markup on success,
     * and caches the updated search result by the new resultIndex.
     */
    @PostMapping(value = "/reprice", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> reprice(
            @RequestParam(required = false) String sessionId,
            @Valid @RequestBody RepriceRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        if (request.getResultIndex() == null || request.getResultIndex().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getChannel() == null || request.getChannel().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getBookingClasses() == null || request.getBookingClasses().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String finalSessionId = sessionId;
        log.info("[Session: {}] Galileo reprice request for resultIndex: {}, channel: {}, bookingClasses: {}",
                finalSessionId, request.getResultIndex(), request.getChannel(), request.getBookingClasses().size());

        Response response = galileoRepriceService.reprice(finalSessionId, request, traceId);
        return ResponseEntity.ok(response);
    }


    /**
     * Fare rules endpoint. Proxies the request to the core
     * {@code /api/flights/fare-rules} endpoint.
     */
    @PostMapping(value = "/fare-rules", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FareRulesResponse> fareRules(@Valid @RequestBody FareRulesRequest request) {

        if (request.getResultIndex() == null || request.getResultIndex().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getChannel() == null || request.getChannel().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Fare rules request for resultIndex: {}, channel: {}",
                request.getResultIndex(), request.getChannel());

        FareRulesResponse response = fareRulesService.getFareRules(request);
        return ResponseEntity.ok(response);
    }


    /**
     * FlyDubai add-to-cart endpoint. Proxies the request to the GDS
     * {@code /api/flydubai/flights/addToCart} endpoint and wraps the result
     * in the standard {@link BaseResponse} envelope.
     */
    @PostMapping(value = "/addToCart", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse<AddToCartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {

        if (request.getChannel() == null || request.getChannel().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error(400, "channel is required", null));
        }

        if (request.getResultIndex() == null || request.getResultIndex().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error(400, "resultIndex is required", null));
        }

        log.info("FlyDubai addToCart request for resultIndex: {}, channel: {}",
                request.getResultIndex(), request.getChannel());

        AddToCartResponse response = flyDubaiService.addToCart(request);
        return ResponseEntity.ok(BaseResponse.ok("Add to cart successful", response));
    }


    /**
     * Returns the most recent unique flight searches for the authenticated user.
     *
     * GET /api/flights/recent-searches?limit=10
     *
     * @param limit max number of unique results (default 10, max 50)
     * @return list of recent search entries
     */
    @GetMapping(value = "/recent-searches", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<RecentSearchResponse>>> recentSearches(
            @RequestParam(defaultValue = "10") int limit) {

        Long userId = getUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(BaseResponse.error(401, "Unauthorized", null));
        }

        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<RecentSearchResponse> results = searchService.getRecentSearches(userId, safeLimit);
        return ResponseEntity.ok(BaseResponse.ok(results));
    }

}
