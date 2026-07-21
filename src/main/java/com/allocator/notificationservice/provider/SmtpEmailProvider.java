package com.allocator.notificationservice.provider;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import com.allocator.notificationservice.config.NotificationMetrics;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;
    private final NotificationMetrics metrics;

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        log.info("Sending SMTP email to: {}", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            metrics.incrementEmailSent();
        } catch (MessagingException e) {
            metrics.incrementEmailFailed();
            log.error("Failed to send SMTP email to {}", to, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    @Override
    public void sendBulkEmail(List<String> recipients, String subject, String htmlBody) {
        log.info("Sending bulk SMTP email to {} recipients", recipients.size());
        for (String recipient : recipients) {
            sendEmail(recipient, subject, htmlBody);
        }
    }
}
