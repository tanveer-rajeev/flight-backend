package com.aerionsoft.application.service.flight;

import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.dto.flight.farerules.FareRulesRequest;
import com.aerionsoft.application.dto.flight.farerules.FareRulesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class FareRulesService {

    private final WebClient webClient;

    @Value("${flight_api_key}")
    private String apiKey;

    @Value("${flight_api_url}")
    private String apiUrl;

    public FareRulesService(WebClient insecureWebClient) {
        this.webClient = insecureWebClient;
    }

    public FareRulesResponse getFareRules(FareRulesRequest request) {
        String url = apiUrl + "/api/flights/fare-rules";

        log.info("Calling core fare rules for resultIndex: {}, channel: {}",
                request.getResultIndex(), request.getChannel());

        FareRulesResponse response = webClient.post()
                .uri(url)
                .header("x-api-key", apiKey)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FareRulesResponse.class)
                .block();

        if (response == null) {
            log.error("Core fare rules returned null for resultIndex: {}", request.getResultIndex());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Fare rules request returned empty response");
        }

        if (!response.isSuccess()) {
            log.warn("Core fare rules failed for resultIndex {}: {} ({})",
                    request.getResultIndex(), response.getMessage(), response.getReason());
        } else {
            log.info("Fare rules retrieved for resultIndex: {}, provider: {}",
                    request.getResultIndex(), response.getProviderCode());
        }

        return response;
    }
}
