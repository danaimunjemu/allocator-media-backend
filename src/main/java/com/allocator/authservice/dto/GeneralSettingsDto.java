package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralSettingsDto {
    private String platformUrl;
    private String language;
    private String timezone;
    private String currency;
    private boolean maintenanceMode;
}
