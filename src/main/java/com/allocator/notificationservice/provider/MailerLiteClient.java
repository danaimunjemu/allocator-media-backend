package com.allocator.notificationservice.provider;

import com.allocator.authservice.service.IntegrationSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

// Thin wrapper around MailerLite's Connect API (https://connect.mailerlite.com/api).
// Sends newsletters by: upserting each recipient as a subscriber in a
// dedicated group for the send, creating a "regular" campaign targeting that
// group, then triggering an instant send. The API key is read live from
// platform_settings (via IntegrationSettingService) on every call rather than
// cached at startup, so a key saved through Settings > Integrations takes
// effect immediately.
@Slf4j
@Service
@RequiredArgsConstructor
public class MailerLiteClient {

    private static final String BASE_URL = "https://connect.mailerlite.com/api";

    private final IntegrationSettingService integrationSettingService;
    private final RestTemplate mailerLiteRestTemplate;

    public boolean isConfigured() {
        String key = integrationSettingService.getMailerLiteApiKey();
        return key != null && !key.isBlank();
    }

    /** Creates a group scoped to a single send (e.g. "Newsletter: <subject> — 2026-07-17"). Returns the group id. */
    public String createGroup(String name) {
        Map<String, Object> body = Map.of("name", name);
        Map<?, ?> response = exchange(HttpMethod.POST, "/groups", body, Map.class);
        Object data = response != null ? response.get("data") : null;
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new IllegalStateException("MailerLite did not return a group id");
        }
        return String.valueOf(dataMap.get("id"));
    }

    /** Upserts a subscriber and assigns them to the given group in one call. */
    public void upsertSubscriberInGroup(String email, String groupId) {
        Map<String, Object> body = Map.of(
                "email", email,
                "groups", List.of(groupId)
        );
        try {
            exchange(HttpMethod.POST, "/subscribers", body, Map.class);
        } catch (Exception e) {
            log.warn("MailerLite: failed to upsert subscriber {} — skipping", email, e);
        }
    }

    /** Creates a "regular" campaign with the given HTML content targeting one group. Returns the campaign id. */
    public String createCampaign(String name, String subject, String fromName, String fromEmail,
                                  String groupId, String htmlContent) {
        Map<String, Object> body = Map.of(
                "name", name,
                "type", "regular",
                "groups", List.of(groupId),
                "emails", List.of(Map.of(
                        "subject", subject,
                        "from_name", fromName,
                        "from", fromEmail,
                        "content", htmlContent
                ))
        );
        Map<?, ?> response = exchange(HttpMethod.POST, "/campaigns", body, Map.class);
        Object data = response != null ? response.get("data") : null;
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new IllegalStateException("MailerLite did not return a campaign id");
        }
        return String.valueOf(dataMap.get("id"));
    }

    /** Triggers an immediate send of a previously-created campaign. */
    public void sendCampaignNow(String campaignId) {
        Map<String, Object> body = Map.of("delivery", "instant");
        exchange(HttpMethod.POST, "/campaigns/" + campaignId + "/schedule", body, Map.class);
    }

    private <T> T exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        String apiKey = integrationSettingService.getMailerLiteApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MailerLite API key is not configured — set it under Settings > Integrations");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return mailerLiteRestTemplate.exchange(BASE_URL + path, method, new HttpEntity<>(body, headers), responseType).getBody();
    }
}
