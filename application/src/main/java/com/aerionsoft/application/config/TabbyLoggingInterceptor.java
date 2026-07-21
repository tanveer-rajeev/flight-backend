package com.aerionsoft.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TabbyLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TabbyLoggingInterceptor.class);
    private final boolean enabled;

    public TabbyLoggingInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (enabled) {
            log.debug("Tabby request -> {} {} payload={}",
                    request.getMethod(), request.getURI(),
                    new String(body, StandardCharsets.UTF_8));
        }
        ClientHttpResponse response = execution.execute(request, body);
        if (enabled) {
            log.debug("Tabby response <- status={}", response.getStatusCode());
        }
        return response;
    }
}
