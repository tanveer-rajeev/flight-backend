package com.aerionsoft.application.service.common;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.platform.PlatformProviderChannel;
import com.aerionsoft.application.dto.platform.PlatformInfoData;
import com.aerionsoft.application.enums.booking.Provider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformProviderService {

    private static final Logger log = LoggerFactory.getLogger(PlatformProviderService.class);

    private final WebClient webClient;
    private final String platformInfoUrl;
    private final String apiKey;

    private Map<Provider, String> providerChannelMap = Collections.emptyMap();
    private List<PlatformProviderChannel> allChannelEntries = List.of();

    public PlatformProviderService(
            WebClient insecureWebClient,
            @Value("${platform.info.url:https://platform-fly.aerionsoft.com/}") String platformInfoUrl,
            @Value("${flight_api_key:${flight_api_key}}") String apiKey
    ) {
        this.webClient = insecureWebClient;
        this.platformInfoUrl = platformInfoUrl;
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void loadPlatformProviders() {
        String url = platformInfoUrl + "/api/platform-info/me";
        log.info("Loading platform provider map from {}", url);

        BaseResponse<PlatformInfoData> response = webClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header("x-api-key", apiKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<PlatformInfoData>>() {})
                .block();

        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException("Failed to load platform provider map from " + url);
        }

        List<String> providersMap = response.getData().getProvidersMap();
        if (providersMap == null || providersMap.isEmpty()) {
            throw new IllegalStateException("Platform provider map is empty from " + url);
        }

        Map<Provider, String> parsedMap = new EnumMap<>(Provider.class);
        List<PlatformProviderChannel> channelEntries = new ArrayList<>();
        for (String entry : providersMap) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                log.warn("Skipping invalid provider map entry: {}", entry);
                continue;
            }

            String providerName = parts[0].trim();
            String channel = parts[1].trim();
            if (providerName.isEmpty() || channel.isEmpty()) {
                log.warn("Skipping invalid provider map entry: {}", entry);
                continue;
            }

            try {
                Provider provider = Provider.getByName(providerName);
                channelEntries.add(new PlatformProviderChannel(provider, channel));
                if (parsedMap.containsKey(provider)) {
                    log.warn(
                            "Duplicate provider mapping for {}: keeping {} over {}",
                            provider.name(),
                            parsedMap.get(provider),
                            channel
                    );
                    continue;
                }
                parsedMap.put(provider, channel);
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping unknown provider in platform map: {}", providerName);
            }
        }

        if (parsedMap.isEmpty()) {
            throw new IllegalStateException("No valid provider mappings parsed from platform info");
        }

        this.providerChannelMap = Collections.unmodifiableMap(parsedMap);
        this.allChannelEntries = List.copyOf(channelEntries);
        log.info("Loaded {} platform provider channel mappings ({} total channel entries)",
                providerChannelMap.size(), allChannelEntries.size());
    }

    public String getChannelName(String providerValue) {
        if (providerValue == null || providerValue.isBlank()) {
            return null;
        }

        try {
            Provider provider = Provider.getByName(providerValue);
            return providerChannelMap.get(provider);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isConfiguredProvider(String providerValue) {
        if (providerValue == null || providerValue.isBlank()) {
            return false;
        }

        try {
            Provider provider = Provider.getByName(providerValue);
            return providerChannelMap.containsKey(provider);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public Map<Provider, String> getProviderChannelMap() {
        return providerChannelMap;
    }

    /** Every {@code PROVIDER: channel} pair from platform info (includes duplicate providers). */
    public List<PlatformProviderChannel> getAllChannelEntries() {
        return allChannelEntries;
    }
}
