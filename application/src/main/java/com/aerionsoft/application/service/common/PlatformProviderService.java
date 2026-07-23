package com.aerionsoft.application.service.common;

import com.aerionsoft.application.dto.platform.PlatformProviderChannel;
import com.aerionsoft.application.enums.booking.Provider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@DependsOn("platformInfoService")
public class PlatformProviderService {

    private static final Logger log = LoggerFactory.getLogger(PlatformProviderService.class);

    private final PlatformInfoService platformInfoService;

    private Map<Provider, String> providerChannelMap = Collections.emptyMap();
    private List<PlatformProviderChannel> allChannelEntries = List.of();

    public PlatformProviderService(PlatformInfoService platformInfoService) {
        this.platformInfoService = platformInfoService;
    }

    @PostConstruct
    public void loadPlatformProviders() {
        List<String> providersMap = platformInfoService.getPlatformInfo().getProvidersMap();
        if (providersMap == null || providersMap.isEmpty()) {
            throw new IllegalStateException("Platform provider map is empty");
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
