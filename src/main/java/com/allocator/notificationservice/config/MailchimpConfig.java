package com.allocator.notificationservice.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "mailchimp")
@Getter
@Setter
@Slf4j
public class MailchimpConfig {

    private String apiKey;
    private String listId;
    private String serverPrefix = "us1";

    public boolean isConfigured() {
        boolean configured = StringUtils.hasText(apiKey) && StringUtils.hasText(listId);
        if (!configured) {
            log.warn("Mailchimp is not fully configured (mailchimp.api-key or mailchimp.list-id missing) — running in degraded mode");
        }
        return configured;
    }

    public String getBaseUrl() {
        return "https://" + serverPrefix + ".api.mailchimp.com/3.0";
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
