package com.allocator.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationRequestedEvent extends BaseEvent {
    private String userId;
    private String notificationType;
    private String templateName;
    private Map<String, Object> templateModel;
}
