package com.allocator.authservice.service;

import com.allocator.authservice.dto.LoginHistoryDto;
import com.allocator.authservice.dto.SessionDto;
import com.allocator.authservice.dto.TrustedDeviceDto;
import com.allocator.authservice.model.LoginHistory;
import com.allocator.authservice.model.TrustedDevice;
import com.allocator.authservice.model.UserSession;
import com.allocator.authservice.repository.LoginHistoryRepository;
import com.allocator.authservice.repository.TrustedDeviceRepository;
import com.allocator.authservice.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final UserSessionRepository sessionRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final GeoIpService geoIpService;

    @Transactional
    public void createSession(UUID userId, HttpServletRequest request, String token) {
        String tokenHash = hashToken(token);
        UserAgentInfo ua = parseUserAgent(request.getHeader("User-Agent"));
        String ip = resolveIp(request);

        UserSession session = UserSession.builder()
                .userId(userId)
                .sessionToken(tokenHash)
                .deviceName(ua.deviceName)
                .deviceType(ua.deviceType)
                .os(ua.os)
                .browser(ua.browser)
                .ipAddress(ip)
                .location(geoIpService.resolveLocation(ip))
                .lastActive(LocalDateTime.now())
                .active(true)
                .build();
        sessionRepository.save(session);
        log.debug("Session created for user {}, IP {}", userId, ip);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getActiveSessions(UUID userId, String currentTokenHash) {
        return sessionRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(s -> SessionDto.builder()
                        .id(s.getId())
                        .deviceName(s.getDeviceName())
                        .deviceType(s.getDeviceType())
                        .os(s.getOs())
                        .browser(s.getBrowser())
                        .ipAddress(s.getIpAddress())
                        .location(s.getLocation())
                        .lastActive(s.getLastActive())
                        .createdAt(s.getCreatedAt())
                        .active(s.isActive())
                        .current(s.getSessionToken() != null && s.getSessionToken().equals(currentTokenHash))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void terminateSession(UUID userId, UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            if (!s.getUserId().equals(userId)) {
                throw new SecurityException("Cannot terminate another user's session");
            }
            s.setActive(false);
            sessionRepository.save(s);
        });
    }

    @Transactional
    public void terminateAllSessions(UUID userId, String currentTokenHash) {
        List<UserSession> sessions = sessionRepository.findByUserIdAndActiveTrue(userId);
        for (UserSession s : sessions) {
            if (!s.getSessionToken().equals(currentTokenHash)) {
                s.setActive(false);
                sessionRepository.save(s);
            }
        }
    }

    @Transactional
    public void recordLoginHistory(UUID userId, String email, HttpServletRequest request, boolean success, String failureReason) {
        UserAgentInfo ua = parseUserAgent(request.getHeader("User-Agent"));
        String ip = resolveIp(request);
        String deviceInfo = ua.os + " / " + ua.browser;

        LoginHistory history = LoginHistory.builder()
                .userId(userId)
                .email(email)
                .ipAddress(ip)
                .deviceInfo(deviceInfo)
                .location(geoIpService.resolveLocation(ip))
                .success(success)
                .failureReason(failureReason)
                .loginAt(LocalDateTime.now())
                .build();
        loginHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryDto> getLoginHistory(UUID userId) {
        return loginHistoryRepository.findByUserIdOrderByLoginAtDesc(userId).stream()
                .map(h -> LoginHistoryDto.builder()
                        .id(h.getId())
                        .email(h.getEmail())
                        .ipAddress(h.getIpAddress())
                        .deviceInfo(h.getDeviceInfo())
                        .location(h.getLocation())
                        .success(h.isSuccess())
                        .failureReason(h.getFailureReason())
                        .loginAt(h.getLoginAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrustedDeviceDto> getTrustedDevices(UUID userId) {
        return trustedDeviceRepository.findByUserId(userId).stream()
                .map(d -> TrustedDeviceDto.builder()
                        .id(d.getId())
                        .deviceName(d.getDeviceName())
                        .deviceType(d.getDeviceType())
                        .os(d.getOs())
                        .deviceFingerprint(d.getDeviceFingerprint())
                        .trustedAt(d.getTrustedAt())
                        .lastUsedAt(d.getLastUsedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public TrustedDeviceDto trustDevice(UUID userId, String fingerprint, String deviceName, String deviceType, String os) {
        Optional<TrustedDevice> existing = trustedDeviceRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint);
        TrustedDevice device = existing.orElseGet(() -> TrustedDevice.builder()
                .userId(userId)
                .deviceFingerprint(fingerprint)
                .deviceName(deviceName)
                .deviceType(deviceType)
                .os(os)
                .trustedAt(LocalDateTime.now())
                .build());
        device.setLastUsedAt(LocalDateTime.now());
        TrustedDevice saved = trustedDeviceRepository.save(device);
        return TrustedDeviceDto.builder()
                .id(saved.getId())
                .deviceName(saved.getDeviceName())
                .deviceType(saved.getDeviceType())
                .os(saved.getOs())
                .deviceFingerprint(saved.getDeviceFingerprint())
                .trustedAt(saved.getTrustedAt())
                .lastUsedAt(saved.getLastUsedAt())
                .build();
    }

    @Transactional
    public void removeTrustedDevice(UUID userId, UUID deviceId) {
        trustedDeviceRepository.deleteByUserIdAndId(userId, deviceId);
    }

    public boolean isDeviceTrusted(UUID userId, String fingerprint) {
        return trustedDeviceRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint).isPresent();
    }

    @Transactional
    public void updateLastActive(String tokenHash) {
        sessionRepository.findBySessionToken(tokenHash).ifPresent(s -> {
            s.setLastActive(LocalDateTime.now());
            sessionRepository.save(s);
        });
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UserAgentInfo parseUserAgent(String ua) {
        if (ua == null) return new UserAgentInfo("Unknown", "DESKTOP", "Unknown", "Unknown");

        String deviceType = "DESKTOP";
        String deviceName = "Desktop";
        String os = "Unknown";
        String browser = "Unknown";

        if (ua.contains("iPhone")) {
            deviceType = "MOBILE";
            deviceName = "iPhone";
            os = "iOS";
        } else if (ua.contains("iPad")) {
            deviceType = "TABLET";
            deviceName = "iPad";
            os = "iPadOS";
        } else if (ua.contains("Android")) {
            deviceType = "MOBILE";
            deviceName = "Android Device";
            os = "Android";
        } else if (ua.contains("Macintosh")) {
            deviceType = "DESKTOP";
            deviceName = "MacBook";
            os = "macOS";
        } else if (ua.contains("Windows NT")) {
            deviceType = "DESKTOP";
            deviceName = "Windows PC";
            os = "Windows";
        } else if (ua.contains("Linux")) {
            deviceType = "DESKTOP";
            deviceName = "Linux PC";
            os = "Linux";
        }

        if (ua.contains("Edg/")) {
            browser = "Edge";
        } else if (ua.contains("OPR/") || ua.contains("Opera/")) {
            browser = "Opera";
        } else if (ua.contains("Chrome/")) {
            browser = "Chrome";
        } else if (ua.contains("Firefox/")) {
            browser = "Firefox";
        } else if (ua.contains("Safari/")) {
            browser = "Safari";
        }

        return new UserAgentInfo(deviceName, deviceType, os, browser);
    }

    private record UserAgentInfo(String deviceName, String deviceType, String os, String browser) {}
}
