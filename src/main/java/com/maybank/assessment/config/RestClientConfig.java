package com.maybank.assessment.config;

import com.maybank.assessment.logging.OutboundRequestLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient exchangeRateRestClient(ExchangeRateProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                // Buffering lets the logging interceptor read the body without consuming it.
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .requestInterceptor(new OutboundRequestLoggingInterceptor("ExchangeRateAPI"))
                .build();
    }
}
