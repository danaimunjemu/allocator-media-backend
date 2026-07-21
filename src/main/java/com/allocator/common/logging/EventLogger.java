package com.allocator.common.logging;

import com.allocator.common.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class EventLogger {

    public static final String MDC_EVENT_ID = "eventId";
    public static final String MDC_EVENT_TYPE = "eventType";
    public static final String MDC_TOPIC = "topic";
    public static final String MDC_SOURCE_SERVICE = "sourceService";
    public static final String MDC_EVENT_TIMESTAMP = "eventTimestamp";
    public static final String MDC_RESILIENCE_SERVICE = "serviceName";
    public static final String MDC_RESILIENCE_OPERATION = "operation";
    public static final String MDC_RESILIENCE_FAILURE_REASON = "failureReason";

    public static void logProducedEvent(BaseEvent event, String topic) {
        try {
            setupMdc(event, topic);
            log.info("Produced Event: {} to topic: {}", event.getEventType(), topic);
        } finally {
            clearMdc();
        }
    }

    public static void logConsumedEvent(BaseEvent event, String topic) {
        try {
            setupMdc(event, topic);
            log.info("Consumed Event: {} from topic: {}", event.getEventType(), topic);
        } finally {
            clearMdc();
        }
    }

    public static void logResilienceEvent(String service, String operation, String message, String failureReason) {
        try {
            MDC.put(MDC_RESILIENCE_SERVICE, service);
            MDC.put(MDC_RESILIENCE_OPERATION, operation);
            if (failureReason != null) {
                MDC.put(MDC_RESILIENCE_FAILURE_REASON, failureReason);
            }
            log.warn("Resilience Event: {} - {} - {}", service, operation, message);
        } finally {
            MDC.remove(MDC_RESILIENCE_SERVICE);
            MDC.remove(MDC_RESILIENCE_OPERATION);
            MDC.remove(MDC_RESILIENCE_FAILURE_REASON);
        }
    }

    private static void setupMdc(BaseEvent event, String topic) {
        MDC.put(MDC_EVENT_ID, event.getEventId());
        MDC.put(MDC_EVENT_TYPE, event.getEventType());
        MDC.put(MDC_TOPIC, topic);
        MDC.put(MDC_SOURCE_SERVICE, event.getSourceService());
        if (event.getTimestamp() != null) {
            MDC.put(MDC_EVENT_TIMESTAMP, event.getTimestamp().toString());
        }
    }

    private static void clearMdc() {
        MDC.remove(MDC_EVENT_ID);
        MDC.remove(MDC_EVENT_TYPE);
        MDC.remove(MDC_TOPIC);
        MDC.remove(MDC_SOURCE_SERVICE);
        MDC.remove(MDC_EVENT_TIMESTAMP);
    }
}
