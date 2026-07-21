package com.allocator.mediaservice.dto.event;

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
public class MediaEvent {
    private UUID eventId;
    private String eventType;
    private UUID aggregateId;
    private Instant timestamp;
    private Object payload;
}
