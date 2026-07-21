package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaConfigDto {
    private boolean totpEnabled;
    private boolean emailMfaEnabled;
    private boolean backupCodesEnabled;
    private String preferredMethod;
    private boolean mfaEnabled;
}
