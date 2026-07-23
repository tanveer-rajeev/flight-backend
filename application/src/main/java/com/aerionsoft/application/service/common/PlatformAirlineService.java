package com.aerionsoft.application.service.common;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.AirlineDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class PlatformAirlineService {

    private static final Logger log = LoggerFactory.getLogger(PlatformAirlineService.class);

    private final WebClient webClient;
    private final String platformInfoUrl;
    private final String apiKey;

    private List<AirlineDto> airlines = List.of();

    public PlatformAirlineService(
            WebClient insecureWebClient,
            @Value("${platform.info.url:https://platform-fly.aerionsoft.com/}") String platformInfoUrl,
            @Value("${flight_api_key:${flight_api_key}}") String apiKey
    ) {
        this.webClient = insecureWebClient;
        this.platformInfoUrl = platformInfoUrl;
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void loadAirlines() {
        airlines = fetchAirlines();
    }

    public List<AirlineDto> getAirlines() {
        return airlines;
    }

    private List<AirlineDto> fetchAirlines() {
        String url = platformInfoUrl + "/api/airline/list";
        log.info("Loading airlines from {}", url);

        BaseResponse<List<AirlineDto>> response = webClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header("x-api-key", apiKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<AirlineDto>>>() {})
                .block();

        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException("Failed to load airlines from " + url);
        }

        return List.copyOf(response.getData());
    }
}
