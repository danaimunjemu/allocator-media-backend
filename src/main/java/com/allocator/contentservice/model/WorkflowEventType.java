package com.allocator.contentservice.model;

public enum WorkflowEventType {
    // Content lifecycle
    CREATED,
    UPDATED,
    AUTOSAVED,
    SUBMITTED,
    RETURNED,           // editorial rejection — returned to DRAFT
    APPROVED,
    REJECTED,           // alias kept for backward compat; prefer RETURNED for editorial, COMPLIANCE_REJECTED for compliance
    SCHEDULED,
    SCHEDULE_FAILED,    // auto-publish job could not execute
    PUBLISHED,
    UNPUBLISHED,
    ARCHIVED,
    RESTORED,
    BRAND_CHANGED,
    // Versioning
    VERSION_CREATED,
    VERSION_PUBLISHED,
    VERSION_ARCHIVED,
    // Compliance
    COMPLIANCE_APPROVED,
    COMPLIANCE_REJECTED,
    // Approval lifecycle
    APPROVAL_RETRACTED,
    // Collaboration — comments
    COMMENT_ADDED,
    REPLY_ADDED,
    COMMENT_RESOLVED,
    COMMENT_REOPENED,
    COMMENT_DELETED,
    // Collaboration — annotations
    ANNOTATION_CREATED,
    ANNOTATION_RESOLVED,
    // Collaboration — review requests
    REVIEW_REQUESTED,
    REVIEW_COMPLETED,
    // Collaboration — tasks
    TASK_CREATED,
    TASK_ASSIGNED,
    TASK_COMPLETED,
    TASK_CANCELLED,
    // Collaboration — edit locks
    EDIT_LOCK_ACQUIRED,
    EDIT_LOCK_RELEASED
}
