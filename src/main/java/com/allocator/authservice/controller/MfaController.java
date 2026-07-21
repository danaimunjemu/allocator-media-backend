package com.allocator.authservice.controller;

import com.allocator.authservice.dto.*;
import com.allocator.authservice.service.AuthService;
import com.allocator.authservice.service.MfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;
    private final AuthService authService;

    @PostMapping("/api/v1/auth/mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfa(@RequestBody MfaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request));
    }

    @GetMapping("/api/v1/users/mfa")
    public ResponseEntity<MfaConfigDto> getMfaConfig() {
        String email = getCurrentEmail();
        return ResponseEntity.ok(mfaService.getMfaConfig(email));
    }

    @PostMapping("/api/v1/users/mfa/totp/setup")
    public ResponseEntity<MfaSetupResponse> setupTotp() {
        String email = getCurrentEmail();
        return ResponseEntity.ok(mfaService.setupTotp(email));
    }

    @PostMapping("/api/v1/users/mfa/totp/enable")
    public ResponseEntity<Void> enableTotp(@RequestBody java.util.Map<String, String> body) {
        String email = getCurrentEmail();
        mfaService.enableTotp(email, body.get("code"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/users/mfa/totp/disable")
    public ResponseEntity<Void> disableTotp() {
        String email = getCurrentEmail();
        mfaService.disableTotp(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/users/mfa/email/toggle")
    public ResponseEntity<Void> toggleEmailMfa(@RequestBody java.util.Map<String, Boolean> body) {
        String email = getCurrentEmail();
        mfaService.toggleEmailMfa(email, Boolean.TRUE.equals(body.get("enabled")));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/v1/users/mfa/backup-codes")
    public ResponseEntity<BackupCodesResponse> getBackupCodeStatus() {
        String email = getCurrentEmail();
        return ResponseEntity.ok(mfaService.getBackupCodeStatus(email));
    }

    @PostMapping("/api/v1/users/mfa/backup-codes")
    public ResponseEntity<BackupCodesResponse> generateBackupCodes() {
        String email = getCurrentEmail();
        return ResponseEntity.ok(mfaService.generateBackupCodes(email));
    }

    @PostMapping("/api/v1/users/mfa/backup-codes/regenerate")
    public ResponseEntity<BackupCodesResponse> regenerateBackupCodes() {
        String email = getCurrentEmail();
        return ResponseEntity.ok(mfaService.regenerateBackupCodes(email));
    }

    private String getCurrentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
