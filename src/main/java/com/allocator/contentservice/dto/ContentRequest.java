package com.allocator.contentservice.dto;

import com.allocator.contentservice.model.AccessTier;
import com.allocator.contentservice.model.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRequest {

    @NotNull
    private UUID brandId;

    private List<UUID> authorIds;
    private Boolean anonymous;

    private List<String> mediaIds;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String subtitle;

    @NotBlank
    @Size(max = 255)
    private String slug;

    @Size(max = 500)
    private String summary;

    private String body;

    private String heroImage;

    private String readTime;

    private UUID categoryId;

    @NotNull
    private ContentType contentType;

    @Size(max = 255)
    private String metaTitle;

    @Size(max = 500)
    private String metaDescription;

    @Size(max = 500)
    private String canonicalUrl;

    private Map<String, Object> metadata;

    private List<String> tags;

    private List<MediaRequest> media;

    /** Subscription tier required to access full content. Defaults to FREE. */
    private AccessTier accessTier;

    private String excerpt;

    private List<UUID> referenceIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaRequest {
        private UUID mediaId;
        private String type;
        private String url;
        private String altText;
        private Integer sortOrder;
    }
}
