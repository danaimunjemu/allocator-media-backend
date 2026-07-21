package com.allocator.contentservice.config;

import com.allocator.common.logging.EventLogger;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration("contentResilienceLoggingConfig")
public class ResilienceLoggingConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;

    @Value("${spring.application.name}")
    private String serviceName;

    public ResilienceLoggingConfig(CircuitBreakerRegistry circuitBreakerRegistry,
                                  RetryRegistry retryRegistry,
                                  BulkheadRegistry bulkheadRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
    }

    @PostConstruct
    public void init() {
        // Log existing circuit breakers and subscribe to new ones
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::subscribeToCircuitBreakerEvents);
        circuitBreakerRegistry.getEventPublisher().onEntryAdded(event -> subscribeToCircuitBreakerEvents(event.getAddedEntry()));

        // Log existing retries and subscribe to new ones
        retryRegistry.getAllRetries().forEach(this::subscribeToRetryEvents);
        retryRegistry.getEventPublisher().onEntryAdded(event -> subscribeToRetryEvents(event.getAddedEntry()));

        // Log existing bulkheads and subscribe to new ones
        bulkheadRegistry.getAllBulkheads().forEach(this::subscribeToBulkheadEvents);
        bulkheadRegistry.getEventPublisher().onEntryAdded(event -> subscribeToBulkheadEvents(event.getAddedEntry()));
    }

    private void subscribeToCircuitBreakerEvents(io.github.resilience4j.circuitbreaker.CircuitBreaker cb) {
        cb.getEventPublisher().onStateTransition(event -> {
            EventLogger.logResilienceEvent(serviceName, "CircuitBreaker-" + event.getCircuitBreakerName(),
                    "State transition: " + event.getStateTransition(), null);
        });
        cb.getEventPublisher().onError(event -> {
            EventLogger.logResilienceEvent(serviceName, "CircuitBreaker-" + event.getCircuitBreakerName(),
                    "Error recorded", event.getThrowable().getMessage());
        });
    }

    private void subscribeToRetryEvents(io.github.resilience4j.retry.Retry retry) {
        retry.getEventPublisher().onRetry(event -> {
            EventLogger.logResilienceEvent(serviceName, "Retry-" + event.getName(),
                    "Retry attempt: " + event.getNumberOfRetryAttempts(),
                    event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : null);
        });
        retry.getEventPublisher().onSuccess(event -> {
            EventLogger.logResilienceEvent(serviceName, "Retry-" + event.getName(),
                    "Retry success after " + event.getNumberOfRetryAttempts() + " attempts", null);
        });
    }

    private void subscribeToBulkheadEvents(io.github.resilience4j.bulkhead.Bulkhead bulkhead) {
        bulkhead.getEventPublisher().onCallRejected(event -> {
            EventLogger.logResilienceEvent(serviceName, "Bulkhead-" + event.getBulkheadName(),
                    "Call rejected", "Bulkhead is full");
        });
    }
}

