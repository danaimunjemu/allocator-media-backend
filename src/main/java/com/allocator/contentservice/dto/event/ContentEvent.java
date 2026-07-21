package com.allocator.contentservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEvent {
    private UUID eventId;
    private String eventType;
    private String aggregateType;
    private UUID aggregateId;
    private UUID brandId;
    private Instant timestamp;
    private Object payload;
}
