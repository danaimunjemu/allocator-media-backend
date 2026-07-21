package com.allocator.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MediaUploadedEvent extends BaseEvent {
    private String fileId;
    private String fileName;
    private String url;
    private String uploaderId;
    private String contentType;
    private long sizeBytes;
}
