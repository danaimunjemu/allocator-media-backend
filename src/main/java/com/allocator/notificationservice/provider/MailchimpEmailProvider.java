package com.allocator.notificationservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "mailchimp")
public class MailchimpEmailProvider implements EmailProvider {

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        log.info("Sending Mailchimp email to: {} (Simulation)", to);
        // TODO: Implement Mailchimp API integration
    }

    @Override
    public void sendBulkEmail(List<String> recipients, String subject, String htmlBody) {
        log.info("Sending bulk Mailchimp email to {} recipients (Simulation)", recipients.size());
        // TODO: Implement Mailchimp bulk API integration
    }
}
