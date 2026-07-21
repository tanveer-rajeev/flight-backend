package com.aerionsoft.application.service.booking;
import com.aerionsoft.application.service.common.PlatformProviderService;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.service.admin.GroupTicketService;
import com.aerionsoft.application.service.flight.MarkupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BookingPriceService {

    private static final String REDIS_KEY_VALIDATION = "validation:price:";

    private final StringRedisTemplate redisTemplate;
    private final GroupTicketService groupTicketService;
    private final MarkupService markupService;
    private final PlatformProviderService platformProviderService;

    @Autowired
    public BookingPriceService(StringRedisTemplate redisTemplate,
                               GroupTicketService groupTicketService,
                               MarkupService markupService,
                               PlatformProviderService platformProviderService) {
        this.redisTemplate = redisTemplate;
        this.groupTicketService = groupTicketService;
        this.markupService = markupService;
        this.platformProviderService = platformProviderService;
    }


    public Double getBookingPrice(String resultIndex, String providerName, String channel) {
        if (resultIndex == null || resultIndex.isBlank()) {
            throw new IllegalArgumentException("Result index cannot be null or blank");
        }

        if ("group".equalsIgnoreCase(providerName)) {
            return 0.0;
        }
        // price form validation key in Redis (final price)
        String redisKey = buildRedisKey(resultIndex, providerName,channel);
        String priceStr = redisTemplate.opsForValue().get(redisKey);

        if (priceStr != null) {
            try {
                return Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                throw ServiceExceptions.internal("Invalid price format in Redis. Please try again.", e);
            }
        }
        // Fallback to Caffeine cache for Flydubai beacuse it does not have validation key in Redis

        if ("flydubai".equalsIgnoreCase(providerName)) {
            MarkupService.MarkupData markupData = markupService.getStoredMarkupData(resultIndex);

            if (markupData != null) {
                double bookingPrice = markupData.resolvedBookingPrice();

                log.info(
                        "Flydubai booking price from markup cache (no Redis validation key): resultIndex={}, price={}",
                        resultIndex,
                        bookingPrice
                );

                return bookingPrice;
            }
        }

        throw ServiceExceptions.notFound("Price not found for selected flight. Please try again. provider: " + providerName);
    }

    /**
     * Get the original price (before markup) from local Caffeine cache.
     */
    public Double getOriginalPrice(String resultIndex, String providerName) {
        if (resultIndex == null || resultIndex.isBlank()) {
            return null;
        }

        if (providerName != null && providerName.equalsIgnoreCase("group")) {
            return groupTicketService.getPriceByGfCode(resultIndex);
        }

        // Get from local Caffeine cache (instant)
        MarkupService.MarkupData markupData = markupService.getStoredMarkupData(resultIndex);
        return markupData != null ? markupData.originalPrice() : null;
    }

    /**
     * Get the markup amount from local Caffeine cache.
     */
    public Double getMarkupAmount(String resultIndex, String providerName) {
        if (resultIndex == null || resultIndex.isBlank()) {
            return 0.0;
        }

        if (providerName != null && providerName.equalsIgnoreCase("group")) {
            return 0.0;
        }

        // Get from local Caffeine cache (instant)
        MarkupService.MarkupData markupData = markupService.getStoredMarkupData(resultIndex);
        return markupData != null ? markupData.totalMarkup() : 0.0;
    }

    /**
     * Get the buy price (supplier cost after commission provision) from local Caffeine cache.
     */
    public Double getBuyPrice(String resultIndex, String providerName) {
        if (resultIndex == null || resultIndex.isBlank()) {
            return null;
        }

        if (providerName != null && providerName.equalsIgnoreCase("group")) {
            return groupTicketService.getCostingPriceByGfCode(resultIndex, 1);
        }

        MarkupService.MarkupData markupData = markupService.getStoredMarkupData(resultIndex);
        return markupData != null ? markupData.resolvedBuyPrice() : null;
    }

    /**
     * Get full booking price details from Redis (final price) and Caffeine cache (markup data).
     */
    public BookingPriceDetails getBookingPriceDetails(String resultIndex, String providerName,String channel) {
        Double bookingPrice = getBookingPrice(resultIndex, providerName,channel);
        Double originalPrice = getOriginalPrice(resultIndex, providerName);
        Double markupAmount = getMarkupAmount(resultIndex, providerName);
        Double buyPrice = getBuyPrice(resultIndex, providerName);

        return new BookingPriceDetails(bookingPrice, originalPrice, markupAmount, buyPrice);
    }

    private String buildRedisKey(String resultIndex, String providerName, String channel) {
        if (!platformProviderService.isConfiguredProvider(providerName)) {
            throw ServiceExceptions.business("Unsupported provider: " + providerName);
        }
        return REDIS_KEY_VALIDATION + resultIndex;
    }

    /**
     * DTO to hold booking price details.
     */
    public record BookingPriceDetails(Double bookingPrice, Double originalPrice, Double markupAmount, Double buyPrice) {
        public BookingPriceDetails {
            if (bookingPrice == null || bookingPrice < 0) {
                throw new IllegalArgumentException("Booking price must be positive");
            }
            if (originalPrice == null) {
                originalPrice = bookingPrice;
            }
            if (markupAmount == null) {
                markupAmount = 0.0;
            }
            if (buyPrice == null) {
                buyPrice = originalPrice;
            }
        }
    }
}
