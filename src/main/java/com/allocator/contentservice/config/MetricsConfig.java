package com.allocator.contentservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("contentMetricsConfig")
public class MetricsConfig {

    @Bean
    public Counter contentCreatedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("content.created")
                .description("Number of content items created")
                .register(meterRegistry);
    }

    @Bean
    public Counter contentPublishedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("content.published")
                .description("Number of content items published")
                .register(meterRegistry);
    }

    @Bean
    public Counter contentScheduledCounter(MeterRegistry meterRegistry) {
        return Counter.builder("content.scheduled")
                .description("Number of content items scheduled")
                .register(meterRegistry);
    }

    @Bean
    public Counter contentFailedWorkflowCounter(MeterRegistry meterRegistry) {
        return Counter.builder("content.failed_workflow")
                .description("Number of failed workflow transitions")
                .register(meterRegistry);
    }
}

