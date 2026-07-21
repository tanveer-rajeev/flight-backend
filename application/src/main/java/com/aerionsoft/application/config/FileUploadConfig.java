package com.aerionsoft.application.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class FileUploadConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        // Set max file size
        factory.setMaxFileSize(DataSize.ofMegabytes(20));

        // Set max request size
        factory.setMaxRequestSize(DataSize.ofMegabytes(20));

        // Set file size threshold (when to write to disk)
        factory.setFileSizeThreshold(DataSize.ofKilobytes(2));

        return factory.createMultipartConfig();
    }
}

