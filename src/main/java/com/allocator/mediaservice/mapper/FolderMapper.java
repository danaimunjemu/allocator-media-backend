package com.allocator.mediaservice.mapper;

import com.allocator.mediaservice.dto.FolderResponse;
import com.allocator.mediaservice.model.MediaFolder;
import com.allocator.mediaservice.repository.MediaFolderRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.IterableMapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class FolderMapper {

    @Autowired
    private MediaFolderRepository folderRepository;

    @Named("simple")
    public abstract FolderResponse toResponse(MediaFolder folder);

    @IterableMapping(qualifiedByName = "simple")
    public abstract List<FolderResponse> toResponseList(List<MediaFolder> folders);

    @Mapping(target = "childFolders", source = "id", qualifiedByName = "mapChildFolders")
    @Mapping(target = "fileCount", source = "id", qualifiedByName = "mapFileCount")
    public abstract FolderResponse toResponseWithDetails(MediaFolder folder);

    @Named("mapChildFolders")
    protected List<FolderResponse> mapChildFolders(UUID folderId) {
        List<MediaFolder> childFolders = folderRepository.findByParentFolder_Id(folderId);
        return toResponseList(childFolders);
    }

    @Named("mapFileCount")
    protected Integer mapFileCount(UUID folderId) {
        return folderRepository.countFilesInFolder(folderId).intValue();
    }
}
