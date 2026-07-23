package com.aerionsoft.application.service.common;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.platform.PlatformInfoData;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PlatformInfoService {

    private static final Logger log = LoggerFactory.getLogger(PlatformInfoService.class);

    private final WebClient webClient;
    private final String platformInfoUrl;
    private final String apiKey;

    private PlatformInfoData platformInfo;

    public PlatformInfoService(
            WebClient insecureWebClient,
            @Value("${platform.info.url:https://platform-fly.aerionsoft.com/}") String platformInfoUrl,
            @Value("${flight_api_key:${flight_api_key}}") String apiKey
    ) {
        this.webClient = insecureWebClient;
        this.platformInfoUrl = platformInfoUrl;
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void loadPlatformInfo() {
        platformInfo = fetchPlatformInfo();
    }

    public PlatformInfoData getPlatformInfo() {
        if (platformInfo == null) {
            throw new IllegalStateException("Platform info is not loaded");
        }
        return platformInfo;
    }

    private PlatformInfoData fetchPlatformInfo() {
        String url = platformInfoUrl + "/api/platform-info/me";
        log.info("Loading platform info from {}", url);

        BaseResponse<PlatformInfoData> response = webClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header("x-api-key", apiKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<PlatformInfoData>>() {})
                .block();

        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException("Failed to load platform info from " + url);
        }

        return response.getData();
    }
}
