package com.allocator.paymentservice.config;

import com.allocator.authservice.repository.PlatformSettingRepository;
import com.allocator.authservice.security.SecretEncryptionService;
import com.allocator.authservice.service.IntegrationSettingService;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StripeConfig {

    private final PlatformSettingRepository platformSettingRepository;
    private final SecretEncryptionService secretEncryptionService;

    @Value("${stripe.secret-key}")
    private String envSecretKey;

    @PostConstruct
    public void init() {
        String dbKey = platformSettingRepository.findBySettingKey(IntegrationSettingService.KEY_STRIPE_SECRET_KEY)
                .map(setting -> setting.getSettingValue())
                .filter(value -> value != null && !value.isBlank())
                .map(this::tryDecrypt)
                .orElse(null);

        Stripe.apiKey = dbKey != null ? dbKey : envSecretKey;
        log.info("Stripe SDK initialized (source: {})", dbKey != null ? "database" : "env var");
    }

    // app.jwt.secret derives the encryption key (see SecretEncryptionService), so a
    // key stored under a previous JWT secret becomes undecryptable after it changes.
    // Degrade to the env/yaml fallback instead of failing application startup — same
    // pattern as GeoIpService for a missing GeoLite2 file.
    private String tryDecrypt(String encrypted) {
        try {
            return secretEncryptionService.decrypt(encrypted);
        } catch (Exception e) {
            log.warn("Failed to decrypt stored Stripe secret key (likely app.jwt.secret changed since it was saved) — " +
                    "falling back to stripe.secret-key. Re-save the key via Settings > Integrations to fix this.", e);
            return null;
        }
    }
}
