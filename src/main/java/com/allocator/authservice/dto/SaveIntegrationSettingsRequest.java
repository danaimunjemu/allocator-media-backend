package com.allocator.authservice.dto;

import lombok.Data;

/** Only fields that are present and non-blank are updated — omitted fields leave the stored key untouched. */
@Data
public class SaveIntegrationSettingsRequest {
    private String stripeSecretKey;
    private String mailerliteApiKey;
    private String youtubeApiKey;
    private String mansaApiKey;
}
