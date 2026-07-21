package com.allocator.contentservice.dto.event;

import com.allocator.contentservice.model.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEventPayload {
    private UUID contentId;
    private UUID brandId;
    private String title;
    private String slug;
    private ContentType contentType;
    private String fromStatus;
    private String toStatus;
    private UUID actorId;
    private String actorRole;
    private String reason;
    private Instant publishedAt;
    private Instant scheduledAt;
    private List<String> tags;
    private Map<String, Object> metadata;
}
