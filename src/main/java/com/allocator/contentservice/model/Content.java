package com.allocator.contentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "contents", indexes = {
    @Index(name = "idx_content_slug", columnList = "slug", unique = true),
    @Index(name = "idx_content_brand_id", columnList = "brand_id"),
    @Index(name = "idx_content_status", columnList = "status"),
    @Index(name = "idx_content_published_at", columnList = "published_at"),
    @Index(name = "idx_content_category_id", columnList = "category_id")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"references", "authors", "media", "revisions"})
public class Content extends BaseEntity {

    @NotNull
    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "title", nullable = false)
    private String title;

    @Size(max = 255)
    @Column(name = "subtitle")
    private String subtitle;

    @NotBlank
    @Size(max = 255)
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Size(max = 500)
    @Column(name = "summary")
    private String summary;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "hero_image_url")
    private String heroImageUrl;

    @Column(name = "read_time")
    private String readTime;

    // Real FK to categories(id) — see migration 019. Nullable: category
    // assignment is optional and not every ContentType uses it (only
    // ARTICLE has a category picker in the admin editor today).
    @Column(name = "category_id")
    private UUID categoryId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    // Editorial fields — EAGER so authors are always included in every Content load
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "content_author_links",
        joinColumns = @JoinColumn(name = "content_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Builder.Default
    private List<Author> authors = new ArrayList<>();

    @Builder.Default
    @Column(name = "anonymous", nullable = false)
    private Boolean anonymous = false;

    @Column(name = "editor_id")
    private UUID editorId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContentStatus status;

    // Workflow timestamp and actor fields — immutable once set
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Publishing fields
    @Column(name = "published_at")
    private Instant publishedAt;

    // Scheduled auto-publish time — used when status = APPROVED and a future publish date is set.
    // The scheduler publishes content automatically when scheduledAt <= NOW() and status = APPROVED.
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "scheduled_by")
    private UUID scheduledBy;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Builder.Default
    @Column(name = "featured", nullable = false)
    private Boolean featured = false;

    // Monetisation: metadata field consumed by gateway/BFF for access control.
    // Content-service exposes this field but does NOT enforce access restrictions.
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "access_tier", nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'FREE'")
    private AccessTier accessTier = AccessTier.FREE;

    @Builder.Default
    @Column(name = "highlighted", nullable = false)
    private Boolean highlighted = false;

    // SEO fields
    @Size(max = 255)
    @Column(name = "meta_title")
    private String metaTitle;

    @Size(max = 500)
    @Column(name = "meta_description")
    private String metaDescription;

    @Size(max = 500)
    @Column(name = "canonical_url")
    private String canonicalUrl;

    // Flexible Metadata (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    // Simplified Tags
    @ElementCollection
    @CollectionTable(name = "content_tags", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "tag")
    private List<String> tags;

    @ElementCollection
    @CollectionTable(name = "content_media_ids", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "media_id")
    private List<String> mediaIds;

    // Remaining Relationships
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentMedia> media;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentRevision> revisions;

    @Column(name = "excerpt", columnDefinition = "TEXT")
    private String excerpt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "content_reference_links",
        joinColumns = @JoinColumn(name = "content_id"),
        inverseJoinColumns = @JoinColumn(name = "reference_id")
    )
    @Builder.Default
    private Set<Reference> references = new HashSet<>();

    // ── Versioning ───────────────────────────────────────────────────────────
    // versionNumber starts at 1 on the original draft; each createContentRevision increments it.
    @Builder.Default
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    // Points to the original (root) content record — null for root records.
    @Column(name = "parent_content_id")
    private UUID parentContentId;

    // Only one record per parent group has latestVersion=true at any time.
    @Builder.Default
    @Column(name = "latest_version", nullable = false)
    private Boolean latestVersion = true;

    // ── Compliance ───────────────────────────────────────────────────────────
    // Derived from contentType via CompliancePolicy but can be overridden per item.
    @Builder.Default
    @Column(name = "compliance_required", nullable = false)
    private Boolean complianceRequired = false;

    @Column(name = "compliance_approved")
    private Boolean complianceApproved;

    @Column(name = "compliance_approved_by")
    private UUID complianceApprovedBy;

    @Column(name = "compliance_approved_at")
    private Instant complianceApprovedAt;
}
