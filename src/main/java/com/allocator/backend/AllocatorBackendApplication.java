package com.allocator.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        scanBasePackages = "com.allocator",
        exclude = {org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class}
)
@EnableJpaRepositories(basePackages = "com.allocator")
@EntityScan(basePackages = "com.allocator")
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class AllocatorBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllocatorBackendApplication.class, args);
    }
}
