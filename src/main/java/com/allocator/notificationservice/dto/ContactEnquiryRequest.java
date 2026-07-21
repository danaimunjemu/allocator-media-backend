package com.allocator.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound DTO for a contact-form submission from a public-facing site.
 * The BFF layer is responsible for injecting {@code brandId} before
 * forwarding the request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactEnquiryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must not exceed 120 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 10, message = "Message must be at least 10 characters")
    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    private String message;

    /** Brand identifier — injected by the BFF; never sent from browser JS. */
    private String brandId;
}
