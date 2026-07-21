package com.allocator.authservice.service;

import com.allocator.authservice.dto.BackupCodesResponse;
import com.allocator.authservice.dto.MfaConfigDto;
import com.allocator.authservice.dto.MfaSetupResponse;
import com.allocator.authservice.model.BackupCode;
import com.allocator.authservice.model.MfaConfig;
import com.allocator.authservice.model.User;
import com.allocator.authservice.repository.BackupCodeRepository;
import com.allocator.authservice.repository.MfaConfigRepository;
import com.allocator.authservice.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final MfaConfigRepository mfaConfigRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    @Transactional(readOnly = true)
    public MfaConfigDto getMfaConfig(String email) {
        User user = getUser(email);
        MfaConfig config = getOrCreateConfig(user.getId());
        return MfaConfigDto.builder()
                .totpEnabled(config.isTotpEnabled())
                .emailMfaEnabled(config.isEmailMfaEnabled())
                .backupCodesEnabled(config.isBackupCodesEnabled())
                .preferredMethod(config.getPreferredMethod())
                .mfaEnabled(user.isMfaEnabled())
                .build();
    }

    @Transactional
    public MfaSetupResponse setupTotp(String email) {
        User user = getUser(email);
        String secret = secretGenerator.generate();
        String encryptedSecret = encrypt(secret);

        MfaConfig config = getOrCreateConfig(user.getId());
        config.setTotpSecret(encryptedSecret);
        mfaConfigRepository.save(config);

        String qrCodeUri = buildTotpUri(email, secret);
        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUri(qrCodeUri)
                .method("TOTP")
                .build();
    }

    @Transactional
    public void enableTotp(String email, String code) {
        User user = getUser(email);
        MfaConfig config = getOrCreateConfig(user.getId());

        if (config.getTotpSecret() == null) {
            throw new IllegalStateException("TOTP not set up. Call setup first.");
        }

        String decryptedSecret = decrypt(config.getTotpSecret());
        if (!codeVerifier.isValidCode(decryptedSecret, code)) {
            throw new IllegalArgumentException("Invalid TOTP code");
        }

        config.setTotpEnabled(true);
        config.setPreferredMethod("TOTP");
        mfaConfigRepository.save(config);

        user.setMfaEnabled(true);
        userRepository.save(user);
        log.info("TOTP enabled for user {}", user.getId());
    }

    @Transactional
    public void disableTotp(String email) {
        User user = getUser(email);
        MfaConfig config = getOrCreateConfig(user.getId());
        config.setTotpEnabled(false);
        config.setTotpSecret(null);
        if (!config.isEmailMfaEnabled()) {
            config.setPreferredMethod("NONE");
            user.setMfaEnabled(false);
            userRepository.save(user);
        }
        mfaConfigRepository.save(config);
        log.info("TOTP disabled for user {}", user.getId());
    }

    @Transactional
    public void toggleEmailMfa(String email, boolean enabled) {
        User user = getUser(email);
        MfaConfig config = getOrCreateConfig(user.getId());
        config.setEmailMfaEnabled(enabled);
        if (enabled && "NONE".equals(config.getPreferredMethod())) {
            config.setPreferredMethod("EMAIL");
            user.setMfaEnabled(true);
            userRepository.save(user);
        } else if (!enabled && "EMAIL".equals(config.getPreferredMethod())) {
            config.setPreferredMethod(config.isTotpEnabled() ? "TOTP" : "NONE");
            if (!config.isTotpEnabled()) {
                user.setMfaEnabled(false);
                userRepository.save(user);
            }
        }
        mfaConfigRepository.save(config);
    }

    @Transactional
    public BackupCodesResponse generateBackupCodes(String email) {
        User user = getUser(email);
        backupCodeRepository.deleteByUserId(user.getId());

        List<String> plainCodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String code = generateBackupCode();
            plainCodes.add(code);
            BackupCode backupCode = BackupCode.builder()
                    .userId(user.getId())
                    .codeHash(passwordEncoder.encode(code))
                    .build();
            backupCodeRepository.save(backupCode);
        }

        MfaConfig config = getOrCreateConfig(user.getId());
        config.setBackupCodesEnabled(true);
        mfaConfigRepository.save(config);

        log.info("Generated 10 backup codes for user {}", user.getId());
        return BackupCodesResponse.builder()
                .codes(plainCodes)
                .totalCodes(10)
                .usedCodes(0)
                .remainingCodes(10)
                .build();
    }

    @Transactional
    public BackupCodesResponse regenerateBackupCodes(String email) {
        return generateBackupCodes(email);
    }

    @Transactional(readOnly = true)
    public BackupCodesResponse getBackupCodeStatus(String email) {
        User user = getUser(email);
        List<BackupCode> all = backupCodeRepository.findByUserId(user.getId());
        int used = (int) all.stream().filter(BackupCode::isUsed).count();
        return BackupCodesResponse.builder()
                .codes(null)
                .totalCodes(all.size())
                .usedCodes(used)
                .remainingCodes(all.size() - used)
                .build();
    }

    public boolean verifyTotpCode(UUID userId, String code) {
        MfaConfig config = mfaConfigRepository.findByUserId(userId).orElse(null);
        if (config == null || config.getTotpSecret() == null) return false;
        String decryptedSecret = decrypt(config.getTotpSecret());
        return codeVerifier.isValidCode(decryptedSecret, code);
    }

    @Transactional
    public boolean verifyBackupCode(UUID userId, String code) {
        List<BackupCode> unusedCodes = backupCodeRepository.findByUserIdAndUsedFalse(userId);
        for (BackupCode bc : unusedCodes) {
            if (passwordEncoder.matches(code, bc.getCodeHash())) {
                bc.setUsed(true);
                bc.setUsedAt(LocalDateTime.now());
                backupCodeRepository.save(bc);
                return true;
            }
        }
        return false;
    }

    public String getPreferredMethod(UUID userId) {
        return mfaConfigRepository.findByUserId(userId)
                .map(MfaConfig::getPreferredMethod)
                .orElse("NONE");
    }

    private MfaConfig getOrCreateConfig(UUID userId) {
        return mfaConfigRepository.findByUserId(userId)
                .orElseGet(() -> {
                    MfaConfig c = MfaConfig.builder().userId(userId).build();
                    return mfaConfigRepository.save(c);
                });
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private String buildTotpUri(String email, String secret) {
        String issuer = "Allocator";
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=6&period=30",
                URLEncoder.encode(issuer, StandardCharsets.UTF_8),
                URLEncoder.encode(email, StandardCharsets.UTF_8),
                secret,
                URLEncoder.encode(issuer, StandardCharsets.UTF_8));
    }

    private String generateBackupCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i == 4 || i == 8) sb.append('-');
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private byte[] getAesKey() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(keyBytes, 16);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive AES key", e);
        }
    }

    private String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(getAesKey(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decrypt(String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(getAesKey(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
