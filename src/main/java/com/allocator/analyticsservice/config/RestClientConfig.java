package com.allocator.analyticsservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    /**
     * Standard RestTemplate for client calls.
     */
    @Bean
    public RestTemplate internalRestTemplate() {
        return new RestTemplate();
    }
}
