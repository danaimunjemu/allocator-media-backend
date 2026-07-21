package com.allocator.notificationservice.service;

import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.notificationservice.model.Campaign;
import com.allocator.notificationservice.model.CampaignItem;
import com.allocator.notificationservice.model.CampaignItemType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Renders a Campaign (newsletter) into a single self-contained HTML document
// — inline CSS only, since email clients strip <style> blocks and external
// stylesheets. Used both for the actual email send and for the admin's PDF
// export, so the two always look identical. Newsletters aren't brand-specific
// (they go out to every subscriber regardless of which brand they signed up
// under), so the masthead is always Allocator Media's own branding rather
// than a per-campaign brand lookup.
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterHtmlRenderer {

    private static final String PRIMARY = "#1B7832";
    private static final String ACCENT = "#FFD53B";
    private static final String INK = "#111827";
    private static final String MUTED = "#6B7280";
    private static final String BORDER = "#E5E7EB";
    private static final String CREAM = "#FAF4DE";
    private static final String SERIF = "Georgia, 'Times New Roman', Times, serif";
    private static final String SANS = "-apple-system, Helvetica, Arial, sans-serif";

    private static final String ALLOCATOR_MEDIA = "Allocator Media";

    @org.springframework.beans.factory.annotation.Value("${app.public-api-url:http://localhost:8080}")
    private String publicApiUrl;

    private String logoUrl() {
        return publicApiUrl + "/media-service/api/v1/media/files/uploads/allocator-media-logo-stacked.jpg";
    }

    // Fixed reference city for the header's weather snippet (Open-Meteo, no
    // API key required) — same fallback location list the demo site's own
    // WeatherWidget rotates through. A real reading or nothing, never a
    // fabricated number: if the call fails, the segment is simply omitted.
    private static final double WEATHER_LAT = -1.286389;
    private static final double WEATHER_LON = 36.817223;

    private final ContentRepository contentRepository;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String render(Campaign campaign, List<CampaignItem> items, String siteUrl, String unsubscribeUrl) {
        StringBuilder body = new StringBuilder();
        body.append(header(campaign));

        if (campaign.getContent() != null && !campaign.getContent().isBlank()) {
            body.append(introSection(campaign.getContent()));
        }

        for (CampaignItem item : items) {
            if (item.getItemType() == CampaignItemType.SECTION_BREAK) {
                body.append(sectionBreak(item.getLabel()));
            } else if (item.getContentId() != null) {
                Content content = contentRepository.findById(item.getContentId()).orElse(null);
                if (content != null) {
                    body.append(contentCard(content, siteUrl));
                }
            }
        }

        body.append(footer(siteUrl, unsubscribeUrl));

        return wrap(campaign.getSubject(), body.toString());
    }

    private String wrap(String title, String bodyHtml) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<title>" + esc(title) + "</title></head>" +
                "<body style=\"margin:0;padding:0;background:#F4F4F5;font-family:" + SANS + ";color:" + INK + ";\">" +
                "<div style=\"max-width:600px;margin:0 auto;background:#FFFFFF;\">" + bodyHtml + "</div>" +
                "</body></html>";
    }

    private String header(Campaign campaign) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH));
        String weather = fetchWeatherSnippet();

        StringBuilder metaBar = new StringBuilder();
        metaBar.append(today);
        for (String label : List.of("Research Archive", "Video", "Podcasts")) {
            metaBar.append(" &nbsp;&middot;&nbsp; ").append(label);
        }
        if (weather != null) {
            metaBar.append(" &nbsp;&middot;&nbsp; ").append(weather);
        }

        String thumbnail = (campaign.getThumbnailUrl() != null && !campaign.getThumbnailUrl().isBlank())
                ? "<img src=\"" + esc(campaign.getThumbnailUrl()) + "\" alt=\"\" width=\"600\" " +
                  "style=\"width:100%;max-width:600px;height:auto;display:block;\" />"
                : "";

        return "<div style=\"background:" + CREAM + ";padding:28px 24px;text-align:center;\">" +
                "<img src=\"" + logoUrl() + "\" alt=\"" + ALLOCATOR_MEDIA + "\" width=\"180\" " +
                "style=\"width:180px;max-width:60%;height:auto;display:block;margin:0 auto 18px;\" />" +
                "<div style=\"font-family:" + SERIF + ";font-size:24px;line-height:1.3;color:" + INK + ";font-weight:bold;margin:0 0 8px;\">" + esc(campaign.getName()) + "</div>" +
                (campaign.getPreviewText() != null && !campaign.getPreviewText().isBlank()
                        ? "<div style=\"color:" + MUTED + ";font-size:14px;margin-bottom:14px;\">" + esc(campaign.getPreviewText()) + "</div>"
                        : "") +
                "<div style=\"font-family:" + SANS + ";font-size:12px;color:" + MUTED + ";\">" + metaBar + "</div>" +
                "</div>" + thumbnail;
    }

    private String introSection(String introHtmlOrText) {
        // Newsletter intro content is authored as rich text (same TipTap
        // editor as articles) — treated as pre-rendered HTML here, same as
        // how ArticleBody trusts editor-produced content elsewhere. That raw
        // HTML relies on the admin/demo apps' global stylesheets (prose
        // typography, .image-block classes) for its look — none of which
        // exist in an email, so every tag needs its own inline style here.
        String styled = styleForEmail(introHtmlOrText);
        return "<div style=\"padding:24px;font-size:16px;line-height:1.7;color:" + INK + ";font-family:" + SANS + ";\">" + styled + "</div>";
    }

    // Injects inline styles onto tags produced by the rich-text editor so
    // they render correctly in email clients, which strip <style> blocks and
    // ignore external/class-based CSS entirely.
    private String styleForEmail(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().prettyPrint(false);

        for (Element img : doc.select("img")) {
            img.attr("style", "max-width:100%;height:auto;display:block;margin:20px auto;border-radius:4px;");
        }
        for (Element figure : doc.select("figure")) {
            figure.attr("style", "margin:24px 0;text-align:center;");
        }
        for (Element caption : doc.select("figcaption")) {
            caption.attr("style", "font-family:" + SANS + ";font-size:13px;font-style:normal;color:" + MUTED + ";margin-top:8px;");
        }
        for (Element attribution : doc.select("p.image-block__attribution")) {
            attribution.attr("style", "font-family:" + SANS + ";font-size:12px;color:" + MUTED + ";margin-top:4px;");
        }
        for (Element quote : doc.select("blockquote")) {
            quote.attr("style", "border-left:3px solid " + BORDER + ";margin:24px 0;padding:2px 0 2px 16px;" +
                    "font-family:" + SERIF + ";font-style:italic;font-size:18px;color:" + INK + ";");
        }
        Elements h1 = doc.select("h1");
        h1.attr("style", "font-family:" + SERIF + ";font-weight:bold;font-size:26px;line-height:1.3;color:" + INK + ";margin:28px 0 12px;");
        Elements h2 = doc.select("h2");
        h2.attr("style", "font-family:" + SERIF + ";font-weight:bold;font-size:22px;line-height:1.3;color:" + INK + ";margin:26px 0 10px;");
        Elements h3 = doc.select("h3");
        h3.attr("style", "font-family:" + SERIF + ";font-weight:bold;font-size:19px;line-height:1.3;color:" + INK + ";margin:22px 0 8px;");
        for (Element p : doc.select("p")) {
            if (p.hasClass("image-block__attribution")) continue;
            p.attr("style", "margin:0 0 18px;font-size:16px;line-height:1.7;color:" + INK + ";font-family:" + SANS + ";");
        }
        for (Element list : doc.select("ul, ol")) {
            list.attr("style", "margin:0 0 18px;padding-left:24px;font-size:16px;line-height:1.7;color:" + INK + ";font-family:" + SANS + ";");
        }
        for (Element li : doc.select("li")) {
            li.attr("style", "margin-bottom:6px;");
        }
        for (Element link : doc.select("a")) {
            link.attr("style", "color:" + PRIMARY + ";text-decoration:underline;");
        }

        return doc.body().html();
    }

    private String sectionBreak(String label) {
        String heading = (label != null && !label.isBlank())
                ? "<div style=\"text-align:center;font-family:" + SANS + ";font-size:11px;font-weight:bold;letter-spacing:0.08em;text-transform:uppercase;color:" + PRIMARY + ";padding:20px 24px 8px;\">" + esc(label) + "</div>"
                : "";
        return heading + "<div style=\"border-top:2px solid " + ACCENT + ";margin:0 24px;\"></div>";
    }

    private String contentCard(Content content, String siteUrl) {
        String url = (siteUrl != null ? siteUrl : "") + typePath(content) + "/" + content.getSlug();
        String thumb = (content.getHeroImageUrl() != null && !content.getHeroImageUrl().isBlank())
                ? "<td width=\"96\" valign=\"top\" style=\"padding-right:16px;\"><a href=\"" + esc(url) + "\"><img src=\"" + esc(content.getHeroImageUrl()) + "\" width=\"96\" height=\"96\" style=\"width:96px;height:96px;object-fit:cover;border-radius:4px;display:block;\" /></a></td>"
                : "";
        return "<table role=\"presentation\" width=\"100%\" style=\"padding:16px 24px;border-bottom:1px solid " + BORDER + ";\">" +
                "<tr>" + thumb +
                "<td valign=\"top\">" +
                "<div style=\"font-size:10px;font-weight:bold;letter-spacing:0.05em;text-transform:uppercase;color:" + MUTED + ";margin-bottom:4px;\">" + esc(content.getContentType() != null ? content.getContentType().name() : "") + "</div>" +
                "<a href=\"" + esc(url) + "\" style=\"font-family:" + SERIF + ";font-size:18px;font-weight:bold;color:" + INK + ";text-decoration:none;\">" + esc(content.getTitle()) + "</a>" +
                (content.getSubtitle() != null ? "<div style=\"font-size:13px;color:" + MUTED + ";margin-top:4px;\">" + esc(content.getSubtitle()) + "</div>" : "") +
                "</td></tr></table>";
    }

    private String typePath(Content content) {
        if (content.getContentType() == null) return "/articles";
        return switch (content.getContentType()) {
            case PODCAST -> "/podcasts";
            case VIDEO -> "/videos";
            case RESEARCH -> "/research";
            default -> "/articles";
        };
    }

    private String footer(String siteUrl, String unsubscribeUrl) {
        String site = siteUrl != null ? siteUrl : "#";
        Map<String, String> social = Map.of(
                "X", site + "#",
                "LinkedIn", site + "#",
                "Facebook", site + "#"
        );
        StringBuilder links = new StringBuilder();
        social.forEach((name, href) ->
                links.append("<a href=\"").append(esc(href)).append("\" style=\"color:").append(MUTED)
                        .append(";text-decoration:none;font-size:12px;margin:0 8px;\">").append(esc(name)).append("</a>"));

        return "<div style=\"padding:28px 24px;text-align:center;background:#FAFAFA;border-top:1px solid " + BORDER + ";\">" +
                "<div style=\"margin-bottom:12px;\">" + links + "</div>" +
                "<div style=\"font-size:12px;color:" + MUTED + ";margin-bottom:6px;\">" +
                "<a href=\"" + esc(site) + "\" style=\"color:" + PRIMARY + ";text-decoration:none;\">Visit " + ALLOCATOR_MEDIA + "</a>" +
                "</div>" +
                "<div style=\"font-size:11px;color:" + MUTED + ";\">" +
                "You're receiving this because you subscribed to " + ALLOCATOR_MEDIA + ". " +
                (unsubscribeUrl != null ? "<a href=\"" + esc(unsubscribeUrl) + "\" style=\"color:" + MUTED + ";\">Unsubscribe</a>" : "") +
                "</div></div>";
    }

    // Real Open-Meteo reading for a fixed reference city, or null on any
    // failure — never a fabricated fallback number.
    private String fetchWeatherSnippet() {
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + WEATHER_LAT + "&longitude=" + WEATHER_LON
                    + "&current=temperature_2m,weather_code";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            JsonNode current = objectMapper.readTree(response.body()).path("current");
            if (!current.has("temperature_2m")) return null;

            int tempC = (int) Math.round(current.path("temperature_2m").asDouble());
            int code = current.path("weather_code").asInt();
            return weatherEmoji(code) + " " + tempC + "&deg;C";
        } catch (Exception e) {
            log.debug("Weather snippet unavailable for newsletter header: {}", e.getMessage());
            return null;
        }
    }

    private String weatherEmoji(int code) {
        if (code == 0) return "&#9728;&#65039;";           // ☀️
        if (code <= 3) return "&#9925;&#65039;";            // ⛅️
        if (code == 45 || code == 48) return "&#127787;&#65039;"; // 🌫️
        if (code >= 51 && code <= 67) return "&#127783;&#65039;"; // 🌧️
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "&#10052;&#65039;"; // ❄️
        if (code >= 80 && code <= 82) return "&#127783;&#65039;"; // 🌧️
        if (code >= 95) return "&#9889;"; // ⚡
        return "&#9729;&#65039;"; // ☁️
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
