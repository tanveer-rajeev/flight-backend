package com.aerionsoft.application.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatFileUploadConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> fileUploadSizeCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            // Set the file count max to a higher value
            // This prevents FileCountLimitExceededException when you have many form fields
            context.setAllowCasualMultipartParsing(true);
        });
    }
}

