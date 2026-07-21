package com.aerionsoft.application.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatMultipartConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatFileUploadCustomizer() {
        return factory -> {
            factory.addContextCustomizers(context -> {
                // Enable multipart parsing
                context.setAllowCasualMultipartParsing(true);
            });

            factory.addConnectorCustomizers(connector -> {
                // Increase limits
                connector.setMaxPostSize(20 * 1024 * 1024);
                connector.setProperty("maxParameterCount", "10000");
            });
        };
    }
}
