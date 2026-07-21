package com.allocator.authservice.service;

import com.allocator.authservice.dto.GeneralSettingsDto;
import com.allocator.authservice.model.PlatformSetting;
import com.allocator.authservice.repository.PlatformSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformSettingService {

    private static final String KEY_URL         = "platform.url";
    private static final String KEY_LANGUAGE    = "platform.language";
    private static final String KEY_TIMEZONE    = "platform.timezone";
    private static final String KEY_CURRENCY    = "platform.currency";
    private static final String KEY_MAINTENANCE = "platform.maintenance_mode";

    private final PlatformSettingRepository repo;

    public GeneralSettingsDto getGeneralSettings() {
        return GeneralSettingsDto.builder()
                .platformUrl(get(KEY_URL, "https://allocator.com"))
                .language(get(KEY_LANGUAGE, "en"))
                .timezone(get(KEY_TIMEZONE, "Africa/Johannesburg"))
                .currency(get(KEY_CURRENCY, "USD"))
                .maintenanceMode(Boolean.parseBoolean(get(KEY_MAINTENANCE, "false")))
                .build();
    }

    @Transactional
    public GeneralSettingsDto saveGeneralSettings(GeneralSettingsDto dto) {
        upsert(KEY_URL,         dto.getPlatformUrl());
        upsert(KEY_LANGUAGE,    dto.getLanguage());
        upsert(KEY_TIMEZONE,    dto.getTimezone());
        upsert(KEY_CURRENCY,    dto.getCurrency());
        upsert(KEY_MAINTENANCE, String.valueOf(dto.isMaintenanceMode()));
        return getGeneralSettings();
    }

    private String get(String key, String defaultValue) {
        return repo.findBySettingKey(key)
                .map(PlatformSetting::getSettingValue)
                .orElse(defaultValue);
    }

    private void upsert(String key, String value) {
        PlatformSetting setting = repo.findBySettingKey(key)
                .orElse(PlatformSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        repo.save(setting);
    }
}
