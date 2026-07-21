package com.allocator.notificationservice.provider;

import com.allocator.notificationservice.config.MailchimpConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailchimpAudienceProviderImpl implements MailchimpAudienceProvider {

    private final MailchimpConfig config;
    private final RestTemplate restTemplate;

    @Override
    public Optional<String> addMember(String email, String brandId, boolean consentGiven) {
        if (!consentGiven) {
            log.debug("Skipping Mailchimp sync for {} — consentGiven=false (GDPR)", email);
            return Optional.empty();
        }

        if (!config.isConfigured()) {
            return Optional.empty();
        }

        String subscriberHash = md5Hex(email.toLowerCase());
        String url = config.getBaseUrl() + "/lists/" + config.getListId() + "/members/" + subscriberHash;

        Map<String, Object> body = Map.of(
                "email_address", email,
                "status", "subscribed"
        );

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.PUT,
                    new HttpEntity<>(body, buildHeaders()),
                    Map.class);

            log.info("Mailchimp member upserted for email: {} (status: {})", email, response.getStatusCode());

            if (brandId != null && !brandId.isBlank()) {
                applyTag(subscriberHash, brandId);
            }

            return Optional.of(subscriberHash);

        } catch (HttpClientErrorException e) {
            log.error("Mailchimp API error adding member {}: {} — {}", email, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error syncing member {} to Mailchimp", email, e);
            return Optional.empty();
        }
    }

    @Override
    public void archiveMember(String email) {
        if (!config.isConfigured()) {
            return;
        }

        String subscriberHash = md5Hex(email.toLowerCase());
        String url = config.getBaseUrl() + "/lists/" + config.getListId() + "/members/" + subscriberHash;

        Map<String, Object> body = Map.of("status", "archived");

        try {
            restTemplate.exchange(url, HttpMethod.PATCH,
                    new HttpEntity<>(body, buildHeaders()),
                    Map.class);
            log.info("Mailchimp member archived: {}", email);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED) {
                // 405 means member was never subscribed — acceptable, log and move on
                log.debug("Mailchimp archive skipped for {} — member was never subscribed", email);
            } else {
                log.error("Mailchimp API error archiving member {}: {} — {}", email, e.getStatusCode(), e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.error("Unexpected error archiving member {} in Mailchimp", email, e);
        }
    }

    private void applyTag(String subscriberHash, String brandId) {
        String url = config.getBaseUrl() + "/lists/" + config.getListId()
                + "/members/" + subscriberHash + "/tags";

        Map<String, Object> body = Map.of(
                "tags", List.of(Map.of("name", "brand:" + brandId, "status", "active"))
        );

        try {
            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, buildHeaders()),
                    Void.class);
            log.debug("Applied Mailchimp tag brand:{} to subscriber {}", brandId, subscriberHash);
        } catch (Exception e) {
            log.error("Failed to apply Mailchimp tag brand:{} to subscriber {}", brandId, subscriberHash, e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Mailchimp Basic auth: any username + API key as password
        String credentials = "apikey:" + config.getApiKey();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        return headers;
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
