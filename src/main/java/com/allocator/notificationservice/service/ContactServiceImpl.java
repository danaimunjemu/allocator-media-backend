package com.allocator.notificationservice.service;

import com.allocator.notificationservice.dto.ContactEnquiryRequest;
import com.allocator.notificationservice.model.Notification;
import com.allocator.notificationservice.model.NotificationStatus;
import com.allocator.notificationservice.provider.EmailProvider;
import com.allocator.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Processes contact-form enquiries submitted through a public-facing site.
 *
 * <p>Flow:
 * <ol>
 *   <li>Persist a {@link Notification} record with status {@code PENDING}.</li>
 *   <li>Send an email to the configured internal recipient address.</li>
 *   <li>Update the record to {@code SENT} (or {@code FAILED} on error).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final EmailProvider emailProvider;
    private final NotificationRepository notificationRepository;

    /**
     * Internal address that receives contact enquiries.
     * Defaults to {@code contact@african-allocator.com} if not configured.
     */
    @Value("${app.contact.recipient-email:contact@african-allocator.com}")
    private String internalRecipient;

    @Override
    public Notification submitEnquiry(ContactEnquiryRequest request) {
        log.info("Processing contact enquiry from {} <{}>", request.getName(), request.getEmail());

        String internalSubject = String.format(
                "[Contact Enquiry] %s — %s <%s>",
                request.getSubject(),
                request.getName(),
                request.getEmail()
        );

        String htmlBody = buildHtmlBody(request);

        // Persist with PENDING so a record exists even if email delivery fails
        Notification notification = Notification.builder()
                .email(request.getEmail())
                .subject(internalSubject)
                .content(htmlBody)
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        try {
            emailProvider.sendEmail(internalRecipient, internalSubject, htmlBody);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Contact enquiry delivered — notificationId={}", notification.getId());
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
            log.error("Failed to deliver contact enquiry notificationId={}: {}",
                    notification.getId(), e.getMessage());
            // Re-throw so the controller can respond with 503
            throw new RuntimeException("Failed to send contact enquiry email", e);
        }

        return notification;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildHtmlBody(ContactEnquiryRequest request) {
        // Escape user-supplied content to prevent HTML injection
        String safeName    = escapeHtml(request.getName());
        String safeEmail   = escapeHtml(request.getEmail());
        String safeSubject = escapeHtml(request.getSubject());
        String safeMessage = escapeHtml(request.getMessage()).replace("\n", "<br/>");
        String safeBrandId = request.getBrandId() != null ? escapeHtml(request.getBrandId()) : "—";

        return "<!DOCTYPE html><html><body style=\"font-family:sans-serif;color:#1a1a1a;\">"
                + "<h2 style=\"color:#16a34a;\">New Contact Enquiry</h2>"
                + "<table style=\"border-collapse:collapse;width:100%;max-width:600px;\">"
                + row("From",    safeName + " &lt;" + safeEmail + "&gt;")
                + row("Subject", safeSubject)
                + row("Brand",   safeBrandId)
                + "</table>"
                + "<hr style=\"margin:24px 0;border:none;border-top:1px solid #e5e7eb;\"/>"
                + "<p style=\"white-space:pre-wrap;\">" + safeMessage + "</p>"
                + "</body></html>";
    }

    private static String row(String label, String value) {
        return "<tr>"
                + "<td style=\"padding:8px 12px;font-weight:600;white-space:nowrap;\">" + label + "</td>"
                + "<td style=\"padding:8px 12px;\">" + value + "</td>"
                + "</tr>";
    }

    /**
     * Minimal HTML escaping — prevents XSS when user content is embedded in
     * an HTML email body.
     */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#x27;");
    }
}
