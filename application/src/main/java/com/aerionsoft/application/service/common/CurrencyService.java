package com.aerionsoft.application.service.common;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.dto.CurrencyDto;
import com.aerionsoft.application.entity.Currency;
import com.aerionsoft.application.repository.common.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CurrencyService {

    @Autowired
    private CurrencyRepository currencyRepository;

    public List<Currency> listAll() {
        return list(null, null);
    }

    public List<Currency> list(String provider, String channel) {
        if (provider == null || provider.isBlank()) {
            return currencyRepository.findAllByOrderByIdAsc();
        }
        if (channel == null || channel.isBlank()) {
            return currencyRepository.findByProviderIdAndChannelIsNullOrderByIdAsc(provider);
        }
        return currencyRepository.findByProviderIdAndChannelOrderByIdAsc(provider, channel.trim());
    }

    private static final List<String> COMMON_CURRENCY_CODES = List.of("BDT", "USD", "AED", "INR");

    public List<Currency> listCommonCurrencies() {
        return listCommonCurrencies(null, null);
    }

    public List<Currency> listCommonCurrencies(String provider, String channel) {
        if (provider == null || provider.isBlank()) {
            return currencyRepository.findByCodeInOrderByIdAsc(COMMON_CURRENCY_CODES);
        }

        String normalizedChannel = normalizeChannel(channel);
        if (normalizedChannel == null) {
            return currencyRepository.findByProviderIdAndCodeInOrderByIdAsc(provider, COMMON_CURRENCY_CODES);
        }

        List<Currency> defaults = currencyRepository.findByProviderIdAndChannelIsNullOrderByIdAsc(provider).stream()
                .filter(currency -> COMMON_CURRENCY_CODES.contains(currency.getCode()))
                .toList();

        List<Currency> channelRows = currencyRepository.findByProviderIdAndChannelOrderByIdAsc(
                        provider, normalizedChannel).stream()
                .filter(currency -> COMMON_CURRENCY_CODES.contains(currency.getCode()))
                .toList();

        List<Currency> merged = new ArrayList<>(defaults);
        merged.addAll(channelRows);
        merged.sort(Comparator.comparing(Currency::getId));
        return merged;
    }

    public void create(CurrencyDto currency) {
        String channel = normalizeChannel(currency.getChannel());

        if (currency.getRate() <= 0) {
            throw new IllegalArgumentException("Rate must be greater than zero");
        }

        Currency existingCurrency = channel != null
                ? currencyRepository.findByCodeAndProviderIdAndChannel(currency.getCode(), currency.getProviderName(), channel)
                : currencyRepository.findByCodeAndProviderIdAndChannelIsNull(currency.getCode(), currency.getProviderName());

        if (existingCurrency != null) {
            throw duplicateCurrencyException(currency.getCode(), currency.getProviderName(), channel);
        }

        Currency currencyEntity = Currency.builder()
                .name(currency.getName())
                .code(currency.getCode())
                .rate(currency.getRate())
                .reverseRate(1.0 / currency.getRate())
                .providerId(currency.getProviderName())
                .channel(channel)
                .createdAt(UserDateTimeUtil.now())
                .updatedAt(UserDateTimeUtil.now())
                .build();

        currencyRepository.save(currencyEntity);
    }

    public void bulkCreate(List<CurrencyDto> currencies) {
        for (CurrencyDto currency : currencies) {
            try {
                create(currency);
            } catch (Exception e) {
                System.err.println("Failed to save currency: " + currency.getCode() + " - " + e.getMessage());
            }
        }
    }

    public void update(List<CurrencyDto> currency, String provider) {
        update(currency, provider, null);
    }

    public void update(List<CurrencyDto> currency, String provider, String channel) {
        String normalizedChannel = normalizeChannel(channel);
        List<Currency> currencies = normalizedChannel == null
                ? currencyRepository.findByProviderIdAndChannelIsNullOrderByIdAsc(provider)
                : currencyRepository.findByProviderIdAndChannelOrderByIdAsc(provider, normalizedChannel);

        for (CurrencyDto dto : currency) {
            Currency existingCurrency = currencies.stream()
                    .filter(c -> c.getCode().equals(dto.getCode()))
                    .findFirst()
                    .orElse(null);

            if (existingCurrency != null) {
                existingCurrency.setRate(dto.getRate());
                existingCurrency.setReverseRate(1.0 / dto.getRate());
                existingCurrency.setUpdatedAt(UserDateTimeUtil.now());
                currencyRepository.save(existingCurrency);
            }
        }
    }

    public Double convertCurrency(String amount, String fromCurrency, String toCurrency, String providerId) {
        return convertCurrency(amount, fromCurrency, toCurrency, providerId, null);
    }

    public Double convertCurrency(String amount, String fromCurrency, String toCurrency, String providerId, String channel) {
        double amountValue = Double.parseDouble(amount);

        if (fromCurrency.equals(toCurrency)) {
            return amountValue;
        }

        try {
            Currency fromCurrencyEntity = resolveCurrency(fromCurrency, providerId, channel);
            Currency toCurrencyEntity = resolveCurrency(toCurrency, providerId, channel);

            if (fromCurrencyEntity == null || toCurrencyEntity == null) {
                throw ServiceExceptions.notFound(
                        "Currency conversion rates not found for " + fromCurrency + " to " + toCurrency);
            }

            return amountValue * (toCurrencyEntity.getRate() / fromCurrencyEntity.getRate());
        } catch (NumberFormatException e) {
            throw ServiceExceptions.notFound("Invalid amount format: " + amount);
        }
    }

    public String getExchangeRate(String fromCurrency, String toCurrency, String providerId) {
        return getExchangeRate(fromCurrency, toCurrency, providerId, null);
    }

    public String getExchangeRate(String fromCurrency, String toCurrency, String providerId, String channel) {
        if (fromCurrency.equals(toCurrency)) {
            return "1.00";
        }

        try {
            Currency fromCurrencyEntity = resolveCurrency(fromCurrency, providerId, channel);
            Currency toCurrencyEntity = resolveCurrency(toCurrency, providerId, channel);

            if (fromCurrencyEntity == null || toCurrencyEntity == null) {
                throw ServiceExceptions.notFound(
                        "Currency conversion rates not found for " + fromCurrency + " to " + toCurrency);
            }

            double exchangeRate = toCurrencyEntity.getRate() / fromCurrencyEntity.getRate();
            return String.format("%.4f", exchangeRate);
        } catch (Exception e) {
            throw ServiceExceptions.notFound("Failed to get exchange rate: " + e.getMessage());
        }
    }

    public double getExchangeRateBasedOnUsd(String currency, String providerId) {
        return getExchangeRateBasedOnUsd(currency, providerId, null);
    }

    public double getExchangeRateBasedOnUsd(String currency, String providerId, String channel) {
        try {
            Currency currencyEntity = resolveCurrency(currency, providerId, channel);

            if (currencyEntity == null) {
                throw ServiceExceptions.notFound("Currency conversion rate not found for " + currency);
            }

            return currencyEntity.getRate();
        } catch (Exception e) {
            throw ServiceExceptions.microservice("Failed to get exchange rate based on USD: " + e.getMessage());
        }
    }

    /**
     * Resolves a currency rate for provider + channel + code.
     * Lookup order: channel-specific row, then provider default (null/blank channel).
     */
    private Currency resolveCurrency(String code, String providerId, String channel) {
        String normalizedChannel = normalizeChannel(channel);

        if (normalizedChannel != null) {
            Currency channelSpecific =
                    currencyRepository.findChannelCurrency(code, providerId, normalizedChannel);
            if (channelSpecific != null) {
                return channelSpecific;
            }
        }

        return currencyRepository.findProviderDefaultCurrency(code, providerId);
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        return channel.trim();
    }

    private RuntimeException duplicateCurrencyException(String code, String provider, String channel) {
        if (channel != null) {
            return ServiceExceptions.duplicate(
                    "Currency with code " + code + " already exists for provider " + provider + " and channel " + channel);
        }
        return ServiceExceptions.duplicate(
                "Currency with code " + code + " already exists for provider " + provider);
    }
}
