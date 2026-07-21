package com.allocator.authservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("authMetricsConfig")
public class MetricsConfig {

    @Bean
    public Counter loginSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("auth.login.success")
                .description("Number of successful login attempts")
                .register(meterRegistry);
    }

    @Bean
    public Counter loginFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("auth.login.failure")
                .description("Number of failed login attempts")
                .register(meterRegistry);
    }

    @Bean
    public Counter registerCounter(MeterRegistry meterRegistry) {
        return Counter.builder("auth.register.count")
                .description("Number of user registrations")
                .register(meterRegistry);
    }
}

