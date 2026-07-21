package com.allocator.analyticsservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicAnalytics {

    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private java.util.UUID id;

    @jakarta.persistence.Column(unique = true)
    private String topic;

    private long views;
    private double growthRate; // Used for trending logic
    private java.time.LocalDateTime lastUpdated;
}
