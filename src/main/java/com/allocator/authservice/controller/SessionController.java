package com.allocator.authservice.controller;

import com.allocator.authservice.dto.LoginHistoryDto;
import com.allocator.authservice.dto.SessionDto;
import com.allocator.authservice.dto.TrustedDeviceDto;
import com.allocator.authservice.model.User;
import com.allocator.authservice.repository.UserRepository;
import com.allocator.authservice.security.JwtProvider;
import com.allocator.authservice.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionDto>> getSessions(HttpServletRequest request) {
        User user = getCurrentUser();
        String token = extractToken(request);
        String tokenHash = token != null ? sessionService.hashToken(token) : null;
        return ResponseEntity.ok(sessionService.getActiveSessions(user.getId(), tokenHash));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> terminateSession(@PathVariable UUID sessionId) {
        User user = getCurrentUser();
        sessionService.terminateSession(user.getId(), sessionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> terminateAllSessions(HttpServletRequest request) {
        User user = getCurrentUser();
        String token = extractToken(request);
        String tokenHash = token != null ? sessionService.hashToken(token) : null;
        sessionService.terminateAllSessions(user.getId(), tokenHash);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/login-history")
    public ResponseEntity<List<LoginHistoryDto>> getLoginHistory() {
        User user = getCurrentUser();
        return ResponseEntity.ok(sessionService.getLoginHistory(user.getId()));
    }

    @GetMapping("/trusted-devices")
    public ResponseEntity<List<TrustedDeviceDto>> getTrustedDevices() {
        User user = getCurrentUser();
        return ResponseEntity.ok(sessionService.getTrustedDevices(user.getId()));
    }

    @DeleteMapping("/trusted-devices/{deviceId}")
    public ResponseEntity<Void> removeTrustedDevice(@PathVariable UUID deviceId) {
        User user = getCurrentUser();
        sessionService.removeTrustedDevice(user.getId(), deviceId);
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
