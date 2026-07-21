package com.allocator.mediaservice.repository;

import com.allocator.mediaservice.model.MediaFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaFolderRepository extends JpaRepository<MediaFolder, UUID> {

    Optional<MediaFolder> findByName(String name);

    List<MediaFolder> findByParentFolder_Id(UUID parentFolderId);

    @Query("SELECT f FROM MediaFolder f WHERE f.parentFolder.id IS NULL")
    List<MediaFolder> findRootFolders();

    @Query("SELECT f FROM MediaFolder f WHERE f.parentFolder.id = :parentFolderId")
    List<MediaFolder> findChildFolders(@Param("parentFolderId") UUID parentFolderId);

    @Query("SELECT COUNT(m) FROM MediaFile m WHERE m.folder.id = :folderId")
    Long countFilesInFolder(@Param("folderId") UUID folderId);

    @Query("SELECT f FROM MediaFolder f WHERE f.name = :name AND f.parentFolder.id = :parentFolderId")
    Optional<MediaFolder> findByNameAndParentFolderId(
            @Param("name") String name,
            @Param("parentFolderId") UUID parentFolderId);

    boolean existsByNameAndParentFolder_Id(String name, UUID parentFolderId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MediaFile m WHERE m.folder.id = :folderId")
    boolean hasFiles(@Param("folderId") UUID folderId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM MediaFolder f WHERE f.parentFolder.id = :folderId")
    boolean hasChildFolders(@Param("folderId") UUID folderId);
}
