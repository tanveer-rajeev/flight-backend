package com.aerionsoft.application.controller.common;


import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.CurrencyDto;
import com.aerionsoft.application.entity.Currency;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.service.common.CurrencyService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/currencies")
public class CurrencyController {

    @Autowired
    private CurrencyService currencyService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<Currency>>> list(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String channel) {
        return ResponseEntity.ok(BaseResponse.ok(currencyService.list(provider, channel)));
    }

    @PostMapping("/create")
    public ResponseEntity<BaseResponse<Void>> create(@Valid @RequestBody CurrencyDto currency) {
         if(Provider.getByName(currency.getProviderName()) == null) {
            return ResponseEntity.badRequest().body(BaseResponse.error("Invalid provider name"));
        }
        if (currency.getRate() <= 0) {
            return ResponseEntity.badRequest().body(BaseResponse.error("Rate must be greater than zero"));
        }

        currencyService.create(currency);
        return ResponseEntity.ok(BaseResponse.ok("Currency created successfully"));
    }

    @PostMapping("/bulk-create")
    public ResponseEntity<BaseResponse<Void>> create(@Valid @RequestBody List<CurrencyDto> currencies) {
        currencyService.bulkCreate(currencies);
        return ResponseEntity.ok(BaseResponse.ok("Currencies created successfully"));
    }


    @PutMapping("/bulk-update")
    public ResponseEntity<BaseResponse<Void>> update(
            @Valid @RequestBody List<CurrencyDto> currency,
            @RequestParam String provider,
            @RequestParam(required = false) String channel) {

        if(Provider.getByName(provider) == null) {
            return ResponseEntity.badRequest().body(BaseResponse.error("Invalid provider name"));
        }

        currencyService.update(currency, provider, channel);
        return ResponseEntity.ok(BaseResponse.ok("Currencies updated successfully"));
    }

    @GetMapping("/exchange-rate")
    public ResponseEntity<BaseResponse<String>> getExchangeRate(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam String providerId,
            @RequestParam(required = false) String channel) {

        String exchangeRate = currencyService.getExchangeRate(fromCurrency, toCurrency, providerId, channel);
        return ResponseEntity.ok(BaseResponse.ok(exchangeRate));
    }

}
