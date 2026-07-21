package com.aerionsoft.application.config;

import com.aerionsoft.application.exception.TabbyApiException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Configuration
public class TabbyRestClientConfig {

    private static final String TABBY_REST_CLIENT_BEAN = "tabbyRestClient";

    @Bean(TABBY_REST_CLIENT_BEAN)
    public RestClient tabbyRestClient(TabbyProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(clientHttpRequestFactory(properties))
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + properties.secretKey())
                .requestInterceptor(new TabbyLoggingInterceptor(properties.logRequests()))
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            byte[] body = response.getBody().readAllBytes();
                            String message = new String(body, StandardCharsets.UTF_8);
                            throw new TabbyApiException(
                                    response.getStatusCode().value(),
                                    message
                            );
                        }
                )
                .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory(TabbyProperties properties) {

        var requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(properties.readTimeout().toMillis()))
                .build();

        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeout().toMillis()))
                                .setTimeToLive(TimeValue.ofMinutes(5))
                                .setValidateAfterInactivity(TimeValue.ofSeconds(10))
                                .build()
                )
                .build();

        var httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
