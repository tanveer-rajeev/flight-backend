package com.aerionsoft.application.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultipartConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            // Set max post size to handle large multipart requests
            connector.setMaxPostSize(20 * 1024 * 1024); // 20MB
            connector.setMaxSavePostSize(20 * 1024 * 1024); // 20MB
        });
    }
}
