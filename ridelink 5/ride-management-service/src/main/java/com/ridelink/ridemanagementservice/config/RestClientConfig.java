package com.ridelink.ridemanagementservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient used by DriverServiceClient and other external clients.
 *
 * Uses Spring's RestClient (available since Spring 6 / Spring Boot 3.2+),
 * which is the recommended HTTP client for Spring Boot 4.x.
 */
@Configuration
public class RestClientConfig {

    /**
     * Provides a RestClient.Builder bean.
     * DriverServiceClient injects this and configures the base URL.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
