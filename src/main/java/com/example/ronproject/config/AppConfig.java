package com.example.ronproject.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, FootballApiProperties.class, CorsProperties.class})
public class AppConfig {

    @Bean
    RestClient footballRestClient(RestClient.Builder builder, FootballApiProperties properties) {
        return builder
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeaders(headers -> {
                    if (properties.getApiToken() != null && !properties.getApiToken().isBlank()) {
                        headers.set("X-Auth-Token", properties.getApiToken());
                    }
                })
                .build();
    }
}
