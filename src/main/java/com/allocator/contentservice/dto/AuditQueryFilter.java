package com.allocator.contentservice.dto;

import com.allocator.contentservice.model.WorkflowEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditQueryFilter {

    private UUID contentId;
    private UUID brandId;
    private UUID actorId;
    private WorkflowEventType eventType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant to;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 50;

    @Builder.Default
    private String sortDir = "desc";
}
