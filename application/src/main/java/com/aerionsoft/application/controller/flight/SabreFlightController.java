package com.aerionsoft.application.controller.flight;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.flight.search.Response;
import com.aerionsoft.application.dto.flight.search.v1.SearchRequestV1;
import com.aerionsoft.application.service.flight.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/sabre/flights")
public class SabreFlightController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(SabreFlightController.class);

    @Autowired
    private SearchService searchService;

    /**
     * V1 Search endpoint supporting multiple origin-destination pairs for multi-city flights
     * Passes v1 request format as-is to the backend API
     *
     * @param sessionId Session ID for tracking search
     * @param requestV1 V1 request with multiple origin-destination pairs
     * @param request   HTTP request for extracting client IP and user agent
     * @return Flux of Response objects streamed in NDJSON format
     */
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
}

