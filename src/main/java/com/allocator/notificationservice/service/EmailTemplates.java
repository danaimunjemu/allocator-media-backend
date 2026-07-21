package com.allocator.notificationservice.service;

import java.util.LinkedHashMap;

import static com.allocator.notificationservice.service.TransactionalEmailBuilder.*;

/**
 * Default bodies for the transactional templates in {@link
 * com.allocator.notificationservice.consumer.NotificationEventConsumer}.
 * These are the ones actually sent unless an admin overrides them with a
 * same-named row in {@code email_templates} (see {@link
 * com.allocator.notificationservice.controller.EmailTemplateController}) —
 * so they carry {@code ${...}} placeholder tokens rather than real values;
 * {@link TemplateService#render} substitutes those afterwards.
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static String welcome() {
        String body =
                eyebrow("Welcome") +
                heading("You're in, ${name}! &#128075;") +
                paragraph("Thanks for joining Allocator Media. Your account is ready — dig into independent " +
                        "research, market coverage and analysis built for allocators who do their own thinking.") +
                button("Go to your dashboard", "${link}") +
                smallMuted("Questions along the way? Just reply to this email — a real person reads every message.");
        return wrap("You're in — welcome to Allocator Media", body);
    }

    public static String verification() {
        String body =
                eyebrow("Verify your email") +
                heading("Confirm your email address") +
                greeting("Hi ${name},") +
                paragraph("One quick step left — verify your email address to activate your account and unlock full access.") +
                button("Verify email address", "${link}") +
                smallMuted("This link expires in 24 hours. If the button doesn't work, paste this into your browser:<br>" +
                        "<a href=\"${link}\" style=\"color:" + PRIMARY + ";word-break:break-all;\">${link}</a>") +
                divider() +
                smallMuted("Didn't create this account? You can safely ignore this email.");
        return wrap("Confirm your email to activate your account", body);
    }

    public static String passwordReset() {
        String body =
                eyebrow("Password reset") +
                heading("Reset your password") +
                greeting("Hi ${name},") +
                paragraph("We received a request to reset your password. Click below to choose a new one — " +
                        "this link expires in 1 hour.") +
                button("Reset password", "${link}") +
                smallMuted("If the button doesn't work, paste this into your browser:<br>" +
                        "<a href=\"${link}\" style=\"color:" + PRIMARY + ";word-break:break-all;\">${link}</a>") +
                divider() +
                smallMuted("Didn't request this? Your password is still safe — just ignore this email.");
        return wrap("Reset your Allocator Media password", body);
    }

    public static String subscriptionConfirmed() {
        LinkedHashMap<String, String> rows = rows();
        rows.put("Plan", "${plan}");
        rows.put("Status", "Active");
        rows.put("Renews", "${renewalDate}");

        String body =
                eyebrow("Subscription confirmed") +
                heading("You're all set, ${name}! &#127881;") +
                paragraph("Your <strong>${plan}</strong> subscription is now active. Thanks for backing " +
                        "independent research — here's a quick summary of your plan.") +
                detailTable(null, rows) +
                button("Manage your subscription", "${manageUrl}") +
                smallMuted("You can update your plan or payment details anytime from your account.");
        return wrap("Your ${plan} subscription is confirmed", body);
    }

    public static String paymentReceipt() {
        LinkedHashMap<String, String> rows = rows();
        rows.put("Invoice number", "${invoiceNumber}");
        rows.put("Payment date", "${paymentDate}");
        rows.put("Payment method", "${paymentMethod}");
        rows.put("Plan", "${plan}");
        rows.put("Amount paid", "${amount}");

        String body =
                eyebrow("Payment received") +
                heading("Congratulations, your payment is complete!") +
                greeting("Hi ${name},") +
                paragraph("We're happy to confirm your payment has been processed and your <strong>${plan}</strong> " +
                        "subscription is active. Thanks for supporting independent research.") +
                detailTable("Payment details", rows) +
                button("View invoice", "${invoiceUrl}") +
                smallMuted("Keep this email for your records. Need a hand? Just reply — we're happy to help.");
        return wrap("Your payment receipt — ${plan}", body);
    }

    public static String articlePublished() {
        String body =
                eyebrow("${topic}") +
                heading("${title}") +
                paragraph("${summary}") +
                button("Read the full story", "${link}");
        return wrap("New from Allocator Media: ${title}", body);
    }
}
