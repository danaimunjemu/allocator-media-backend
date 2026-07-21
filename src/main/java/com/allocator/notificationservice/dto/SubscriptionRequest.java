package com.allocator.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Topic is required")
    private String topic;

    /** Brand identifier used for Mailchimp audience segmentation. */
    private String brandId;

    /** GDPR consent — Mailchimp sync only proceeds when true. */
    @Builder.Default
    private boolean consentGiven = false;
}
