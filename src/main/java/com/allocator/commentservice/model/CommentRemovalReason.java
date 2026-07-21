package com.allocator.commentservice.model;

public enum CommentRemovalReason {
    COMMUNITY_GUIDELINES("Violates community guidelines"),
    HATEFUL_CONDUCT("Hateful conduct"),
    HARASSMENT("Harassment or bullying"),
    SPAM("Spam or misleading"),
    MISINFORMATION("Misinformation"),
    EXPLICIT_CONTENT("Explicit or adult content"),
    OFF_TOPIC("Off-topic"),
    OTHER("Other");

    private final String label;

    CommentRemovalReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
