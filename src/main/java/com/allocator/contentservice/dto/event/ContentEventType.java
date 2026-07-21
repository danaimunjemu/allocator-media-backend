package com.allocator.contentservice.dto.event;

public enum ContentEventType {
    // Canonical event names (preferred for new consumers)
    CONTENT_CREATED,
    CONTENT_UPDATED,
    CONTENT_SUBMITTED,
    CONTENT_REJECTED,
    CONTENT_APPROVED,
    CONTENT_SCHEDULED,
    CONTENT_PUBLISHED,
    CONTENT_ARCHIVED,
    CONTENT_RESTORED,
    CONTENT_UNPUBLISHED,

    // Legacy event names — retained for backward compatibility with existing consumers
    ARTICLE_CREATED,
    ARTICLE_UPDATED,
    ARTICLE_SCHEDULED,
    ARTICLE_PUBLISHED,
    ARTICLE_DELETED
}
