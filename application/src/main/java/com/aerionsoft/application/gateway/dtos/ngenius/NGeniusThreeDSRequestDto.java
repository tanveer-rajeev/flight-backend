package com.aerionsoft.application.gateway.dtos.ngenius;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NGeniusThreeDSRequestDto {

    @Builder.Default
    private String deviceChannel = "BRW";

    @Builder.Default
    private String threeDSCompInd = "Y";

    @Builder.Default
    private String notificationURL = "https://admin.kingstartravel.com/";

    @Builder.Default
    private BrowserInfo browserInfo = BrowserInfo.defaultValues();


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BrowserInfo {

        @Builder.Default
        private String browserAcceptHeader = "application/json";

        @Builder.Default
        private boolean browserJavaEnabled = true;

        @Builder.Default
        private String browserLanguage = "en";

        @Builder.Default
        private String browserTZ = "0";

        @Builder.Default
        private String browserUserAgent =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.93 Safari/537.36";

        @Builder.Default
        private String browserColorDepth = "30";

        @Builder.Default
        private String browserScreenHeight = "1055";

        @Builder.Default
        private String browserScreenWidth = "1680";

        @Builder.Default
        private boolean browserJavascriptEnabled = true;

        @Builder.Default
        private String browserIP = "192.168.1.1";

        @Builder.Default
        private String challengeWindowSize = "05";


        // Helper: allow parent class to easily build defaults
        public static BrowserInfo defaultValues() {
            return BrowserInfo.builder().build();
        }
    }
}
