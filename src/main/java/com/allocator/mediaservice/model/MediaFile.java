package com.allocator.mediaservice.model;

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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "media_files", indexes = {
    @Index(name = "idx_media_file_type", columnList = "mediaType"),
    @Index(name = "idx_media_uploaded_at", columnList = "uploadedAt"),
    @Index(name = "idx_media_uploaded_by", columnList = "uploadedBy"),
    @Index(name = "idx_media_folder_id", columnList = "folder_id"),
    @Index(name = "idx_media_file_name", columnList = "fileName")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MediaFile extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Size(max = 100)
    @Column(name = "file_type")
    private String fileType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @NotNull
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Size(max = 500)
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Size(max = 500)
    @Column(name = "url", nullable = false)
    private String url;

    @NotNull
    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @NotNull
    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Size(max = 64)
    @Column(name = "checksum")
    private String checksum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private MediaFolder folder;

    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MediaTag> tags;

    public enum MediaType {
        IMAGE,
        AUDIO,
        VIDEO,
        DOCUMENT
    }
}
