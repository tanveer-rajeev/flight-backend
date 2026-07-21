package com.aerionsoft.application.service.flight;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.dto.flight.MarkupContext;
import com.aerionsoft.application.dto.flight.arabiaBaggage.BaggageValidationWrapper;
import com.aerionsoft.application.dto.flight.arabiaBaggage.Request;
import com.aerionsoft.application.dto.flight.arabiaBaggage.Response;
import com.aerionsoft.application.dto.flight.search.extras.FinalFare;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.service.booking.BookingTimelineService;
import com.aerionsoft.application.entity.MarkupRule;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SSRService {
    private final WebClient webClient;
    private final MarkupService markupService;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;

    private final BookingTimelineService bookingTimelineService;


    @Value("${flight_api_key}")
    private String apiKey;

    @Value("${flight_api_url}")
    private String apiUrl;


    public SSRService(WebClient insecureWebClient,
                      MarkupService markupService,
                      UserRepository userRepository,
                      BusinessRepository businessRepository,
                      BookingTimelineService bookingTimelineService
    ) {
        this.webClient = insecureWebClient;
        this.markupService = markupService;
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.bookingTimelineService = bookingTimelineService;
    }

    public BaggageValidationWrapper validateBaggage(Request request, String sessionId) {
        String url = apiUrl
                + "/api/flights/bundle-price-validation?sessionId=" + sessionId;

        log.info("[Session: {}] Calling core baggage validation for resultIndex: {}", sessionId, request.getResultIndex());

        BaggageValidationWrapper wrapper = webClient.post()
                .uri(url)
                .header("x-api-key", apiKey)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BaggageValidationWrapper.class)
                .block();

        if (wrapper == null || wrapper.getData() == null) {
            log.error("[Session: {}] Core baggage validation returned null or no data", sessionId);
            bookingTimelineService.recordFlightStep(sessionId, request.getResultIndex(),
                    request.getProviderName(), BookingStatus.BUNDLE_VALIDATION_FAILED, false,
                    "Core baggage validation returned null");
            throw ServiceExceptions.validation("Failed to validate baggage with core service");
        }

        if (!wrapper.isSuccess()) {
            log.error("[Session: {}] Core baggage validation failed: {}", sessionId, wrapper.getMessage());
            bookingTimelineService.recordFlightStep(sessionId, request.getResultIndex(),
                    request.getProviderName(), BookingStatus.BUNDLE_VALIDATION_FAILED, false,
                    wrapper.getMessage());
            throw ServiceExceptions.validation("Baggage validation failed: " + wrapper.getMessage());
        }

        Response response = wrapper.getData();

        log.info("[Session: {}] Core response received: transactionId={}, totalFare={}",
                sessionId, response.getTransactionIdentifier(),
                response.getFare() != null ? response.getFare().getTotalFare() : "null");

        // Apply markup to the response
        MarkupContext context = extractMarkupContext();
        Response updatedResponse = applyMarkupToResponse(request, response, context);

        // Update the wrapper with the marked-up response
        wrapper.setData(updatedResponse);

        bookingTimelineService.recordFlightStep(sessionId, request.getResultIndex(),
                request.getProviderName(), BookingStatus.BUNDLE_VALIDATION_SUCCESS, true,
                request.getBundleCode() != null ? "bundleCode=" + request.getBundleCode() : null);

        return wrapper;
    }

    /**
     * Extract MarkupContext from the current security context.
     */
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

                        Long businessId = user.getBusiness() != null
                                ? user.getBusiness().getId()
                                : null;

                        Optional<BusinessEntity> business =
                                businessRepository.findByMotherUserId(user.getId());

                        if (business.isPresent()) {
                            businessId = business.get().getId();
                        }

                        return MarkupContext.agent(user.getId(), businessId);

                    } else {
                        return MarkupContext.authenticatedUser(user.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting markup context: {}", e.getMessage());
        }

        return MarkupContext.guest();
    }

    /**
     * Apply markup rules to the baggage response.
     * Uses the cached search Response (stored during flight search) to run the full
     * MarkupService.isRuleApplicable check (provider, airline, route, cabin class, dates, etc.).
     */
    private Response applyMarkupToResponse(Request request, Response response, MarkupContext context) {
        if (response.getFare() == null) {
            return response;
        }

        String originalPriceStr = response.getFare().getTotalFare();
        if (originalPriceStr == null || originalPriceStr.isBlank()) {
            return response;
        }

        double gdsPrice = parsePrice(originalPriceStr);
        double totalMarkup = 0.0;
        double baseFare = Double.parseDouble(response.getFare().getBaseFare());
        double tax = gdsPrice - baseFare;

        // Retrieve the cached search Response so we can use the full isRuleApplicable logic
        com.aerionsoft.application.dto.flight.search.Response cachedSearchResponse =
                markupService.getSearchResponse(request.getResultIndex());

        if (cachedSearchResponse == null) {
            log.warn("[SSR] No cached search response for resultIndex={}, skipping markup", request.getResultIndex());
            return response;
        }

        // Build a minimal FinalFare for the base-fare range check in isRuleApplicable
        FinalFare fareForCheck = new FinalFare();
        fareForCheck.setBaseFare(baseFare);

        List<MarkupRule> rules = markupService.getApplicableRules(context);
        for (MarkupRule rule : rules) {
            if (markupService.isRuleApplicable(rule, cachedSearchResponse, fareForCheck)) {
                MarkupService.MarkupCalculation calc = markupService.calculateMarkup(baseFare, tax, rule);
                totalMarkup = calc.totalMarkup();
                log.info("[SSR] Markup applied: resultIndex={}, ruleId={}, markup={}", request.getResultIndex(), rule.getId(), totalMarkup);
                double finalPrice = roundToTwoDecimals(gdsPrice + totalMarkup);
                markupService.storeMarkupData(
                        request.getResultIndex() + ":" + request.getBundleCode(),
                        gdsPrice, totalMarkup, finalPrice, calc.buyPrice());
                response.getFare().setOfferFare(formatPrice(finalPrice));
                if (response.getFareBreakdowns() != null) {
                    for (var breakdown : response.getFareBreakdowns()) {
                        if (breakdown.getTotalFare() != null) {
                            double breakdownPrice = parsePrice(breakdown.getTotalFare());
                            double breakdownFinal = roundToTwoDecimals(breakdownPrice + totalMarkup);
                            breakdown.setTotalFare(formatPrice(breakdownFinal));
                        }
                    }
                }
                log.info("[SSR] Markup result: gds={}, markup={}, final={}, buyPrice={}",
                        gdsPrice, totalMarkup, finalPrice, calc.buyPrice());
                return response;
            }
        }

        log.info("[SSR] No applicable markup rule for resultIndex={}", request.getResultIndex());
        return response;
    }


    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isBlank()) return 0.0;
        try {
            return Double.parseDouble(priceStr.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse price: {}", priceStr);
            return 0.0;
        }
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatPrice(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
