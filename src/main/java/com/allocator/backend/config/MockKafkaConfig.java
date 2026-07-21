package com.allocator.backend.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

@Configuration
public class MockKafkaConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ApplicationEventPublisher eventPublisher) {
        // Create an anonymous dummy ProducerFactory to satisfy KafkaTemplate constructor
        ProducerFactory<String, Object> pf = new ProducerFactory<>() {
            @Override
            public org.apache.kafka.clients.producer.Producer<String, Object> createProducer() {
                return null;
            }
        };

        return new KafkaTemplate<>(pf) {
            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
                eventPublisher.publishEvent(data);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, Object data) {
                eventPublisher.publishEvent(data);
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
