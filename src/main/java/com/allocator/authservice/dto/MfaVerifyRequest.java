package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {
    private String mfaPendingToken;
    private String code;
    private String method;
    private String deviceFingerprint;
    private String deviceName;
    private boolean trustDevice;
}
