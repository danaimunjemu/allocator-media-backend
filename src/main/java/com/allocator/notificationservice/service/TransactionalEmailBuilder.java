package com.allocator.notificationservice.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared HTML building blocks for one-to-one transactional email (welcome,
 * verification, password reset, subscription/payment confirmations) — as
 * opposed to {@link NewsletterHtmlRenderer}, which renders the editorial
 * newsletter. Transactional mail carries the product's own brand palette
 * (navy/cream/periwinkle, per the admin and demo frontends' globals.css)
 * rather than the newsletter's masthead styling, and is built from small
 * reusable pieces since every transactional message shares the same
 * header/card/footer shell. All output uses inline styles and table-based
 * layout only — email clients strip &lt;style&gt; blocks and ignore
 * class-based/flex/grid CSS.
 */
public final class TransactionalEmailBuilder {

    public static final String NAVY = "#0D1B2A";
    public static final String CREAM = "#FAF3DD";
    public static final String PAGE_BG = "#F5EFE1";
    public static final String PRIMARY = "#6B7BDB";
    public static final String PRIMARY_FG = "#FFFFFF";
    public static final String INK = "#0D1B2A";
    public static final String MUTED = "#8A7D6A";
    public static final String BORDER = "#E2D9C4";
    public static final String CARD_BG = "#FFFFFF";
    public static final String ROW_BG = "#FAF7EF";

    private static final String SANS =
            "'Manrope', -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif";
    private static final String BRAND_NAME = "Allocator Media";

    private TransactionalEmailBuilder() {
    }

    /** Wraps a body fragment in the full HTML document, brand header and footer. */
    public static String wrap(String preheader, String bodyHtml) {
        return wrap(preheader, bodyHtml, null);
    }

    public static String wrap(String preheader, String bodyHtml, String unsubscribeUrl) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<meta name=\"color-scheme\" content=\"light\">" +
                "<style>@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;600;700;800&display=swap');</style>" +
                "</head>" +
                "<body style=\"margin:0;padding:0;background:" + PAGE_BG + ";font-family:" + SANS + ";\">" +
                (preheader != null && !preheader.isBlank()
                        ? "<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;\">" + esc(preheader) + "</div>"
                        : "") +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + PAGE_BG + ";padding:32px 16px;\">" +
                "<tr><td align=\"center\">" +
                "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:600px;background:" + CARD_BG + ";border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(13,27,42,0.08);\">" +
                "<tr><td>" + brandBar() + "</td></tr>" +
                "<tr><td style=\"padding:36px 32px 8px;\">" + bodyHtml + "</td></tr>" +
                "<tr><td>" + footer(unsubscribeUrl) + "</td></tr>" +
                "</table>" +
                "<div style=\"max-width:600px;padding:20px 8px 0;font-family:" + SANS + ";font-size:11px;color:" + MUTED + ";\">" +
                esc(BRAND_NAME) + " &middot; This is an automated message, please don't reply directly to this email." +
                "</div>" +
                "</td></tr></table>" +
                "</body></html>";
    }

    private static String brandBar() {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + NAVY + ";\">" +
                "<tr><td style=\"padding:20px 32px;\">" +
                "<span style=\"font-family:" + SANS + ";font-size:18px;font-weight:800;letter-spacing:0.02em;color:" + CREAM + ";\">" +
                esc(BRAND_NAME.toUpperCase()) + "</span>" +
                "</td></tr></table>";
    }

    private static String footer(String unsubscribeUrl) {
        String social = "<a href=\"#\" style=\"" + socialIconStyle() + "\">X</a>" +
                "<a href=\"#\" style=\"" + socialIconStyle() + "\">in</a>" +
                "<a href=\"#\" style=\"" + socialIconStyle() + "\">f</a>";

        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + NAVY + ";\">" +
                "<tr><td align=\"center\" style=\"padding:28px 32px;\">" +
                "<div style=\"margin-bottom:14px;\">" + social + "</div>" +
                "<div style=\"font-family:" + SANS + ";font-size:12px;color:" + CREAM + ";opacity:0.75;line-height:1.6;\">" +
                "&copy; " + java.time.Year.now() + " " + esc(BRAND_NAME) + ". All rights reserved." +
                (unsubscribeUrl != null
                        ? "<br><a href=\"" + esc(unsubscribeUrl) + "\" style=\"color:" + CREAM + ";text-decoration:underline;opacity:0.85;\">Unsubscribe</a>"
                        : "") +
                "</div></td></tr></table>";
    }

    private static String socialIconStyle() {
        return "display:inline-block;width:30px;height:30px;line-height:30px;text-align:center;" +
                "margin:0 4px;border-radius:50%;background:rgba(250,243,221,0.12);" +
                "color:" + CREAM + ";font-family:" + SANS + ";font-size:12px;font-weight:700;text-decoration:none;";
    }

    /** Small uppercase eyebrow label above the headline (e.g. "PAYMENT RECEIVED"). */
    public static String eyebrow(String text) {
        return "<div style=\"font-family:" + SANS + ";font-size:12px;font-weight:700;letter-spacing:0.08em;" +
                "text-transform:uppercase;color:" + PRIMARY + ";margin:0 0 10px;\">" + esc(text) + "</div>";
    }

    public static String heading(String text) {
        return "<h1 style=\"margin:0 0 16px;font-family:" + SANS + ";font-size:24px;line-height:1.3;font-weight:800;color:" + INK + ";\">" +
                text + "</h1>";
    }

    public static String greeting(String text) {
        return "<p style=\"margin:0 0 4px;font-family:" + SANS + ";font-size:15px;color:" + MUTED + ";\">" + text + "</p>";
    }

    public static String paragraph(String html) {
        return "<p style=\"margin:0 0 20px;font-family:" + SANS + ";font-size:15px;line-height:1.65;color:" + INK + ";\">" + html + "</p>";
    }

    public static String smallMuted(String html) {
        return "<p style=\"margin:0 0 20px;font-family:" + SANS + ";font-size:13px;line-height:1.6;color:" + MUTED + ";\">" + html + "</p>";
    }

    /** Primary brand-colour call-to-action pill button — never the reference designs' plain black. */
    public static String button(String label, String url) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:8px 0 28px;\">" +
                "<tr><td style=\"border-radius:6px;background:" + PRIMARY + ";\">" +
                "<a href=\"" + esc(url) + "\" style=\"display:inline-block;padding:13px 28px;font-family:" + SANS + ";" +
                "font-size:14px;font-weight:700;color:" + PRIMARY_FG + ";text-decoration:none;border-radius:6px;\">" +
                esc(label) + "</a></td></tr></table>";
    }

    /** The "Detail Payment" / receipt-style key-value box seen in the reference invoice designs. */
    public static String detailTable(String title, Map<String, String> rows) {
        StringBuilder rowsHtml = new StringBuilder();
        for (Map.Entry<String, String> row : rows.entrySet()) {
            rowsHtml.append("<tr>")
                    .append("<td style=\"padding:11px 16px;font-family:").append(SANS).append(";font-size:13px;color:").append(MUTED).append(";border-top:1px solid ").append(BORDER).append(";\">")
                    .append(esc(row.getKey())).append("</td>")
                    .append("<td align=\"right\" style=\"padding:11px 16px;font-family:").append(SANS).append(";font-size:13px;font-weight:700;color:").append(INK).append(";border-top:1px solid ").append(BORDER).append(";\">")
                    .append(row.getValue()).append("</td>")
                    .append("</tr>");
        }
        return (title != null ? "<div style=\"font-family:" + SANS + ";font-size:13px;font-weight:700;color:" + INK + ";margin:8px 0 10px;\">" + esc(title) + "</div>" : "") +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + ROW_BG + ";border:1px solid " + BORDER + ";border-radius:8px;overflow:hidden;margin:0 0 24px;\">" +
                "<tr><td colspan=\"2\" style=\"height:0;\"></td></tr>" +
                rowsHtml +
                "</table>";
    }

    public static LinkedHashMap<String, String> rows() {
        return new LinkedHashMap<>();
    }

    public static String divider() {
        return "<div style=\"border-top:1px solid " + BORDER + ";margin:8px 0 24px;\"></div>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
