package com.allocator.notificationservice.controller;

import com.allocator.notificationservice.dto.ContactEnquiryRequest;
import com.allocator.notificationservice.model.Notification;
import com.allocator.notificationservice.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles contact-form enquiries submitted from public-facing brand sites.
 *
 * <p>The BFF layer (Next.js Route Handler) is expected to inject {@code brandId}
 * before forwarding the request, keeping that value out of browser JavaScript.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * Accepts a contact-form submission and triggers the internal notification
     * workflow.
     *
     * <p>Returns {@code 202 Accepted} because email delivery is non-blocking
     * — the record is persisted synchronously but SMTP may be async.
     *
     * @param request validated contact enquiry data
     * @return acknowledgement body with the generated notification id
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitEnquiry(
            @Valid @RequestBody ContactEnquiryRequest request
    ) {
        Notification notification = contactService.submitEnquiry(request);

        Map<String, Object> body = Map.of(
                "id",          notification.getId().toString(),
                "submittedAt", LocalDateTime.now().toString()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }
}
