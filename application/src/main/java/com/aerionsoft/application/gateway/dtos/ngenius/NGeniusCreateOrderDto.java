package com.aerionsoft.application.gateway.dtos.ngenius;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NGeniusCreateOrderDto {
    private String action = "PURCHASE";
    private Amount amount = new Amount();
    private MerchantAttributes merchantAttributes = new MerchantAttributes();

    @Data
    public static class Amount {
        private String currencyCode = "AED";
        private int value;
    }

    @Data
    public static class MerchantAttributes {
        private String redirectUrl = "https://kingstartravel.com/redirect";
        private String cancelUrl = "https://kingstartravel.com/cancel";
        private boolean skipConfirmationPage = true;
        private boolean skip3DS = true;
    }
}
