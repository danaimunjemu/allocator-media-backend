package com.allocator.contentservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "editorial_annotations", indexes = {
    @Index(name = "idx_ea_content_id",        columnList = "content_id"),
    @Index(name = "idx_ea_annotator_id",      columnList = "annotator_id"),
    @Index(name = "idx_ea_content_type_status", columnList = "content_id, annotation_type, status")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EditorialAnnotation extends BaseEntity {

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "annotator_id", nullable = false)
    private UUID annotatorId;

    @Column(name = "annotator_name", nullable = false, length = 255)
    private String annotatorName;

    @Column(name = "annotator_role", length = 50)
    private String annotatorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "annotation_type", nullable = false, length = 30)
    private AnnotationType annotationType;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "anchor_start")
    private Integer anchorStart;

    @Column(name = "anchor_end")
    private Integer anchorEnd;

    @Column(name = "anchor_text", length = 1000)
    private String anchorText;

    @Column(name = "content_version_number")
    private Integer contentVersionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "anchor_status", length = 20)
    @Builder.Default
    private AnchorStatus anchorStatus = AnchorStatus.VALID;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CommentStatus status = CommentStatus.OPEN;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_note", columnDefinition = "TEXT")
    private String resolvedNote;
}
