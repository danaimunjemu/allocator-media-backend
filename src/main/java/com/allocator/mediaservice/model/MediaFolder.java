package com.allocator.mediaservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "media_folders", indexes = {
    @Index(name = "idx_media_folder_parent", columnList = "parent_folder_id"),
    @Index(name = "idx_media_folder_name", columnList = "name")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MediaFolder extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private MediaFolder parentFolder;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "folder")
    private List<MediaFile> mediaFiles;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parentFolder")
    private List<MediaFolder> childFolders;
}
