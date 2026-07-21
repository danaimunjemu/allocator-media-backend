package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Never carries decrypted secret values — only whether a key is configured and a masked preview. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationSettingsDto {
    private boolean stripeConfigured;
    private String stripeMasked;
    private boolean mailerliteConfigured;
    private String mailerliteMasked;
    private boolean youtubeConfigured;
    private String youtubeMasked;
    private boolean mansaConfigured;
    private String mansaMasked;
}
