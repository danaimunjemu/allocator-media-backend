package com.allocator.notificationservice.provider;

import java.util.List;

public interface EmailProvider {

    void sendEmail(String to, String subject, String htmlBody);

    void sendBulkEmail(List<String> recipients, String subject, String htmlBody);
}
