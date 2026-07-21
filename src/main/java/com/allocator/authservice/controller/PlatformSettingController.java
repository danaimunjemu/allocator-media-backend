package com.allocator.authservice.controller;

import com.allocator.authservice.dto.GeneralSettingsDto;
import com.allocator.authservice.dto.IntegrationSettingsDto;
import com.allocator.authservice.dto.SaveIntegrationSettingsRequest;
import com.allocator.authservice.service.IntegrationSettingService;
import com.allocator.authservice.service.PlatformSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class PlatformSettingController {

    private final PlatformSettingService platformSettingService;
    private final IntegrationSettingService integrationSettingService;

    @GetMapping("/general")
    public ResponseEntity<GeneralSettingsDto> getGeneralSettings() {
        return ResponseEntity.ok(platformSettingService.getGeneralSettings());
    }

    @PutMapping("/general")
    public ResponseEntity<GeneralSettingsDto> saveGeneralSettings(@RequestBody GeneralSettingsDto dto) {
        return ResponseEntity.ok(platformSettingService.saveGeneralSettings(dto));
    }

    @GetMapping("/integrations")
    public ResponseEntity<IntegrationSettingsDto> getIntegrationSettings(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        requireAdmin(roles);
        return ResponseEntity.ok(integrationSettingService.getIntegrationSettings());
    }

    @PutMapping("/integrations")
    public ResponseEntity<IntegrationSettingsDto> saveIntegrationSettings(
            @RequestBody SaveIntegrationSettingsRequest request,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        requireAdmin(roles);
        return ResponseEntity.ok(integrationSettingService.saveIntegrationSettings(request));
    }

    private void requireAdmin(String rolesHeader) {
        boolean isAdmin = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase("ROLE_ADMIN") || r.equalsIgnoreCase("ADMIN")
                        || r.equalsIgnoreCase("ROLE_SUPER_ADMIN") || r.equalsIgnoreCase("SUPER_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
