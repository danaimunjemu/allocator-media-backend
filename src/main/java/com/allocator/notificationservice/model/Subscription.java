package com.allocator.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscription_email", columnList = "email"),
        @Index(name = "idx_subscription_topic", columnList = "topic")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription extends BaseEntity {

    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String topic;

    @Column(name = "brand_id")
    private String brandId;

    @Builder.Default
    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    /** Mailchimp subscriber hash (MD5 of lowercase email) — null until synced. */
    @Column(name = "mailchimp_member_id")
    private String mailchimpMemberId;
}
