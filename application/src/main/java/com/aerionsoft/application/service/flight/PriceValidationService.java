package com.aerionsoft.application.service.flight;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.dto.flight.MarkupContext;
import com.aerionsoft.application.dto.flight.validation.PriceValidationRequest;
import com.aerionsoft.application.dto.flight.validation.PriceValidationResponse;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.entity.MarkupRule;
import com.aerionsoft.application.service.booking.BookingTimelineService;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PriceValidationService {

    private static final String REDIS_KEY_VALIDATION = "validation:price:";

    @Value("${flight_api_key}")
    private String apiKey;

    @Value("${flight_api_url}")
    private String apiUrl;

    private final WebClient webClient;
    private final StringRedisTemplate redisTemplate;
    private final MarkupService markupService;
    private final UserRepository userRepository;

    @Autowired
    private BookingTimelineService bookingTimelineService;

    @Autowired
    public PriceValidationService(WebClient insecureWebClient,
                                  StringRedisTemplate redisTemplate,
                                  MarkupService markupService,
                                  UserRepository userRepository) {
        this.webClient = insecureWebClient;
        this.redisTemplate = redisTemplate;
        this.markupService = markupService;
        this.userRepository = userRepository;
    }

    /**
     * Validate price by calling the core service, applying markup, and storing prices.
     */
    public PriceValidationResponse validatePrice(String sessionId, PriceValidationRequest request) {
        String url = apiUrl + "/api/flights/price-validation?sessionId=" + sessionId;

        log.info("[Session: {}] Calling core price validation for resultIndex: {}", sessionId, request.getResultIndex());

        PriceValidationResponse coreResponse;
        try {
            coreResponse = callCorePriceValidation(url, request);
        } catch (Exception e) {
            bookingTimelineService.recordFlightStep(sessionId, request.getResultIndex(),
                    request.getProviderName(), BookingStatus.VALIDATION_FAILED, false, e.getMessage());
            throw e;
        }

        if (coreResponse == null) {
            log.error("[Session: {}] Core price validation returned null", sessionId);
            bookingTimelineService.recordFlightStep(sessionId, request.getResultIndex(),
                    request.getProviderName(), BookingStatus.VALIDATION_FAILED, false,
                    "Core price validation returned null");
            throw ServiceExceptions.notFound("Failed to validate price with core service");
        }

        log.info("[Session: {}] Core response: status={}, oldPrice={}, newPrice={}",
                sessionId, coreResponse.getStatus(), coreResponse.getOldPrice(), coreResponse.getNewPrice());

        MarkupContext context = extractMarkupContext();
        PriceValidationResponse markedUpResponse = applyMarkupToValidation(sessionId, coreResponse, request, context);

        storePrices(request, markedUpResponse);
        recordValidationTimeline(sessionId, request, markedUpResponse);

        return markedUpResponse;
    }

    private PriceValidationResponse callCorePriceValidation(String url, PriceValidationRequest request) {
        return webClient.post()
                .uri(url)
                .header("x-api-key", apiKey)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PriceValidationResponse.class)
                .block();
    }

    private void recordValidationTimeline(String sessionId, PriceValidationRequest request,
                                          PriceValidationResponse response) {
        BookingStatus status = response.getStatus() != null
                ? response.getStatus()
                : BookingStatus.VALIDATION_SUCCESS;
        boolean successful = status != BookingStatus.VALIDATION_FAILED
                && status != BookingStatus.FAILED;
        String reason = response.getReason() != null ? response.getReason() : response.getMessage();
        bookingTimelineService.recordFlightStep(sessionId, request.getResultIndex(),
                request.getProviderName(), status, successful, reason);
    }

    /**
     * Apply markup rules to the price validation response.
     * First tries to use stored markup from search, falls back to recalculation if not found.
     * For USBANGLAAPI with bundleCode, uses the bundleCode-specific markup.
     */
    private PriceValidationResponse applyMarkupToValidation(String sessionId,
                                                            PriceValidationResponse response,
                                                            PriceValidationRequest request,
                                                            MarkupContext context) {
        // Original price from core (GDS)
        String originalPriceStr = response.getNewPrice();
        if (originalPriceStr == null || originalPriceStr.isBlank()) {
            originalPriceStr = response.getOldPrice();
        }

        double gdsPrice = parsePrice(originalPriceStr);
        double totalMarkup = 0.0;

        // Determine the cache key to use
        String cacheKey = request.getResultIndex();

        // For USBANGLAAPI with bundleCode, use combined key to get bundle-specific markup
        // Using standard String methods to handle empty/blank strings
        String bundleCode = request.getBundleCode();

        if (("USBANGLAAPI".equalsIgnoreCase(request.getProviderName()) || "VERTEIL".equalsIgnoreCase(request.getProviderName()))
                && bundleCode != null && !bundleCode.trim().isEmpty()) {

            cacheKey = request.getResultIndex() + ":" + bundleCode;
            log.info("[Session: {}] Using bundleCode-specific cache key for {}: {}", sessionId, request.getProviderName(), cacheKey);
        }

        // Try to get stored markup data from search (from Caffeine cache)
        MarkupService.MarkupData storedMarkup = markupService.getStoredMarkupData(cacheKey);

        if (storedMarkup != null) {
            totalMarkup = storedMarkup.totalMarkup();
            log.info("[Session: {}] Using stored markup from key {}: {}", sessionId, cacheKey, totalMarkup);
        } else {
//            throw ServiceExceptions.notFound("Markup data not found for price validation. Cache key: " + cacheKey);
            log.warn("[Session: {}] Markup data not found for key {}. Cannot apply markup to price validation.", sessionId, cacheKey);
        }

        // Calculate final price
        double finalPrice = roundToTwoDecimals(gdsPrice + totalMarkup);
        totalMarkup = roundToTwoDecimals(totalMarkup);

        // Set markup information in response
        response.setOriginalPrice(formatPrice(gdsPrice));
        response.setMarkupAmount(formatPrice(totalMarkup));
        response.setFinalPrice(formatPrice(finalPrice));
        response.setNewPrice(formatPrice(finalPrice));

        if (response.getOldPrice() != null) {
            double oldPriceValue = parsePrice(response.getOldPrice());
            response.setOldPrice(formatPrice(oldPriceValue + totalMarkup));
        }

        // Store/update markup data in Caffeine cache for booking (using the same cache key)
        Double buyPrice = storedMarkup != null ? storedMarkup.buyPrice() : gdsPrice;
        markupService.storeMarkupData(cacheKey, gdsPrice, totalMarkup, finalPrice, buyPrice);

        log.info("[Session: {}] Markup applied: gds={}, markup={}, final={}", sessionId, gdsPrice, totalMarkup, finalPrice);

        return response;
    }

    /**
     * Check if a markup rule is applicable for this validation request.
     */
    private boolean isRuleApplicable(MarkupRule rule, PriceValidationRequest request) {
        // Check provider
        if (rule.getProvider() != null && !rule.getProvider().equalsIgnoreCase("Any")) {
            if (request.getProviderName() != null &&
                    !request.getProviderName().equalsIgnoreCase(rule.getProvider())) {
                return false;
            }
        }

        // Add more checks as needed (origin, destination, airline, etc.)
        // For price validation, we may not have full segment info like in search

        return true;
    }

    /**
     * Calculate markup/commission amount based on type (PERCENTAGE or FIXED).
     */
    private double calculateAmount(double base, String type, Double value) {
        if (value == null || type == null) return 0.0;

        if ("PERCENTAGE".equalsIgnoreCase(type)) {
            return (base * value) / 100.0;
        }
        return value; // FIXED
    }

    /**
     * Store final price in Redis for booking creation.
     * Markup data is already stored in Caffeine cache via MarkupService.
     * For USBANGLAAPI with bundleCode, stores with combined key.
     */
    private void storePrices(PriceValidationRequest request, PriceValidationResponse response) {
        // Determine the Redis key to use
        String redisKey = request.getResultIndex();

        // For USBANGLAAPI with bundleCode, use combined key
        if ("USBANGLAAPI".equalsIgnoreCase(request.getProviderName()) &&
                request.getBundleCode() != null && !request.getBundleCode().isBlank()) {
            redisKey = request.getResultIndex() + ":" + request.getBundleCode();
        }

        // Store only final price in Redis (for booking deduction)
        String priceKey = REDIS_KEY_VALIDATION + redisKey;
        redisTemplate.opsForValue().set(priceKey, response.getFinalPrice(), 10, TimeUnit.MINUTES);
        log.info("Stored final price in Redis: key={}, price={}", redisKey, response.getFinalPrice());
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
                    if (user.isAgency() && user.getBusiness() != null) {
                        return MarkupContext.agent(user.getId(), user.getBusiness().getId());
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
}

