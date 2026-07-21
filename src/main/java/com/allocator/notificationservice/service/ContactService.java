package com.allocator.notificationservice.service;

import com.allocator.notificationservice.dto.ContactEnquiryRequest;
import com.allocator.notificationservice.model.Notification;

public interface ContactService {

    /**
     * Handles a contact-form submission.
     *
     * <p>Sends an email to the configured internal recipient address, then
     * persists a {@link Notification} record for audit and tracking.
     *
     * @param request validated contact enquiry data
     * @return the persisted {@link Notification} record
     */
    Notification submitEnquiry(ContactEnquiryRequest request);
}
