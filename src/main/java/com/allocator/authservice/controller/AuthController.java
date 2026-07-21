package com.allocator.authservice.controller;

import com.allocator.authservice.dto.AssignRoleRequest;
import com.allocator.authservice.dto.AuthResponse;
import com.allocator.authservice.dto.ForgotPasswordRequest;
import com.allocator.authservice.dto.LoginRequest;
import com.allocator.authservice.dto.PasswordChangeRequest;
import com.allocator.authservice.dto.RegisterRequest;
import com.allocator.authservice.dto.ResendVerificationRequest;
import com.allocator.authservice.dto.ResetPasswordRequest;
import com.allocator.authservice.dto.TokenRefreshRequest;
import com.allocator.authservice.dto.VerifyEmailRequest;
import com.allocator.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.allocator.authservice.security.oauth2.OAuthExchangeCodeStore oAuthExchangeCodeStore;

    // Exchanges the single-use opaque code an OAuth2 login redirect handed to
    // the frontend for the real token pair. Called server-side (frontend's
    // own /api/auth/oauth-callback route), never directly from the browser.
    @PostMapping("/oauth/exchange")
    public ResponseEntity<AuthResponse> exchangeOAuthCode(@RequestBody java.util.Map<String, String> body) {
        String code = body.get("code");
        AuthResponse response = code != null ? oAuthExchangeCodeStore.consume(code) : null;
        if (response == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles")
    public ResponseEntity<java.util.List<String>> getRoles() {
        return ResponseEntity.ok(
                java.util.Arrays.stream(com.allocator.authservice.model.RoleName.values())
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.toList())
        );
    }

    @PostMapping("/register")
    public ResponseEntity<java.util.Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(java.util.Map.of(
                "message", "User " + request.getUsername() + " created successfully",
                "timestamp", java.time.Instant.now().toString(),
                "status", 200
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        authService.assignRole(request);
        return ResponseEntity.ok("Role assigned successfully");
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(authService.changePassword(email, request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<java.util.Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<java.util.Map<String, Object>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok(java.util.Map.of("message", "If that account exists and is unverified, a new verification email has been sent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(java.util.Map.of("message", "If that email exists, a password reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password reset successfully"));
    }
}
