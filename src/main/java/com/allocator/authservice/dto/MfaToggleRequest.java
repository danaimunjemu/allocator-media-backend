package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaToggleRequest {
    private String method;
    private boolean enabled;
    private String verificationCode;
}
