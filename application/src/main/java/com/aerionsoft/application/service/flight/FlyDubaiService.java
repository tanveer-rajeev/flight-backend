package com.aerionsoft.application.service.flight;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.dto.flight.flydubai.AddToCartRequest;
import com.aerionsoft.application.dto.flight.flydubai.AddToCartResponse;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.service.booking.BookingTimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class FlyDubaiService {

    private final WebClient webClient;
    private final BookingTimelineService bookingTimelineService;

    @Value("${flight_api_key}")
    private String apiKey;

    @Value("${flight_api_url}")
    private String apiUrl;

    public FlyDubaiService(WebClient insecureWebClient, BookingTimelineService bookingTimelineService) {
        this.webClient = insecureWebClient;
        this.bookingTimelineService = bookingTimelineService;
    }

    public AddToCartResponse addToCart(AddToCartRequest request) {
        String url = apiUrl + "/api/flydubai/flights/addToCart";

        log.info("Calling FlyDubai addToCart for resultIndex: {}, channel: {}",
                request.getResultIndex(), request.getChannel());

        AddToCartResponse response = webClient.post()
                .uri(url)
                .header("x-api-key", apiKey)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AddToCartResponse.class)
                .block();

        if (response == null) {
            log.error("FlyDubai addToCart returned null for resultIndex: {}", request.getResultIndex());
            bookingTimelineService.recordFlightStep(null, request.getResultIndex(), request.getChannel(),
                    BookingStatus.ADD_TO_CART_FAILED, false, "Empty response from GDS");
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "FlyDubai addToCart returned empty response");
        }

        if (!response.isSuccess()) {
            log.error("FlyDubai addToCart failed: {}", response.getMessage());
            bookingTimelineService.recordFlightStep(null, request.getResultIndex(), request.getChannel(),
                    BookingStatus.ADD_TO_CART_FAILED, false, response.getMessage());
            throw ServiceExceptions.bookingFailed("FlyDubai addToCart failed: " + response.getMessage());
        }

        log.info("FlyDubai addToCart succeeded for resultIndex: {}", request.getResultIndex());
        bookingTimelineService.recordFlightStep(null, request.getResultIndex(), request.getChannel(),
                BookingStatus.ADD_TO_CART, true, null);
        return response;
    }
}
