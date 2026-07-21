package com.allocator.authservice.service;

import com.allocator.authservice.dto.IntegrationSettingsDto;
import com.allocator.authservice.dto.SaveIntegrationSettingsRequest;
import com.allocator.authservice.model.PlatformSetting;
import com.allocator.authservice.repository.PlatformSettingRepository;
import com.allocator.authservice.security.SecretEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationSettingService {

    public static final String KEY_STRIPE_SECRET_KEY = "integrations.stripe_secret_key";
    private static final String KEY_MAILERLITE_API_KEY = "integrations.mailerlite_api_key";
    private static final String KEY_YOUTUBE_API_KEY = "integrations.youtube_api_key";
    public static final String KEY_MANSA_API_KEY = "integrations.mansa_api_key";

    private final PlatformSettingRepository repo;
    private final SecretEncryptionService encryption;

    public IntegrationSettingsDto getIntegrationSettings() {
        String stripe = getDecrypted(KEY_STRIPE_SECRET_KEY);
        String mailerlite = getDecrypted(KEY_MAILERLITE_API_KEY);
        String youtube = getDecrypted(KEY_YOUTUBE_API_KEY);
        String mansa = getDecrypted(KEY_MANSA_API_KEY);

        return IntegrationSettingsDto.builder()
                .stripeConfigured(stripe != null)
                .stripeMasked(mask(stripe))
                .mailerliteConfigured(mailerlite != null)
                .mailerliteMasked(mask(mailerlite))
                .youtubeConfigured(youtube != null)
                .youtubeMasked(mask(youtube))
                .mansaConfigured(mansa != null)
                .mansaMasked(mask(mansa))
                .build();
    }

    @Transactional
    public IntegrationSettingsDto saveIntegrationSettings(SaveIntegrationSettingsRequest request) {
        if (request.getStripeSecretKey() != null && !request.getStripeSecretKey().isBlank()) {
            upsertEncrypted(KEY_STRIPE_SECRET_KEY, request.getStripeSecretKey());
            com.stripe.Stripe.apiKey = request.getStripeSecretKey();
        }
        if (request.getMailerliteApiKey() != null && !request.getMailerliteApiKey().isBlank()) {
            upsertEncrypted(KEY_MAILERLITE_API_KEY, request.getMailerliteApiKey());
        }
        if (request.getYoutubeApiKey() != null && !request.getYoutubeApiKey().isBlank()) {
            upsertEncrypted(KEY_YOUTUBE_API_KEY, request.getYoutubeApiKey());
        }
        if (request.getMansaApiKey() != null && !request.getMansaApiKey().isBlank()) {
            upsertEncrypted(KEY_MANSA_API_KEY, request.getMansaApiKey());
        }
        return getIntegrationSettings();
    }

    /** Used by MansaMarketDataService — reads live from the DB, no restart needed after a save. */
    public String getMansaApiKey() {
        return getDecrypted(KEY_MANSA_API_KEY);
    }

    /** Used by MailerLiteConfig — reads live from the DB, no restart needed after a save. */
    public String getMailerLiteApiKey() {
        return getDecrypted(KEY_MAILERLITE_API_KEY);
    }

    // app.jwt.secret derives the encryption key (see SecretEncryptionService), so a
    // key stored under a previous JWT secret becomes undecryptable after it changes.
    // Report as "not configured" instead of 500ing the whole settings page — same
    // degrade-gracefully reasoning as StripeConfig.tryDecrypt(). The admin can just
    // re-save that one key via Settings > Integrations to fix it.
    private String getDecrypted(String key) {
        return repo.findBySettingKey(key)
                .map(PlatformSetting::getSettingValue)
                .filter(v -> !v.isBlank())
                .map(value -> tryDecrypt(key, value))
                .orElse(null);
    }

    private String tryDecrypt(String key, String encrypted) {
        try {
            return encryption.decrypt(encrypted);
        } catch (Exception e) {
            log.warn("Failed to decrypt stored value for '{}' (likely app.jwt.secret changed since it was saved) — " +
                    "reporting as not configured. Re-save it via Settings > Integrations to fix this.", key, e);
            return null;
        }
    }

    private void upsertEncrypted(String key, String plaintext) {
        PlatformSetting setting = repo.findBySettingKey(key)
                .orElse(PlatformSetting.builder().settingKey(key).build());
        setting.setSettingValue(encryption.encrypt(plaintext));
        repo.save(setting);
    }

    private String mask(String secret) {
        if (secret == null) {
            return null;
        }
        String tail = secret.length() > 4 ? secret.substring(secret.length() - 4) : secret;
        return "••••••••" + tail;
    }
}
