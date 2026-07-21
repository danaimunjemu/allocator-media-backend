package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupCodesResponse {
    private List<String> codes;
    private int totalCodes;
    private int usedCodes;
    private int remainingCodes;
}
