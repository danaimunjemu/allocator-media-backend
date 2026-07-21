package com.allocator.mediaservice.service;

import com.allocator.mediaservice.dto.FolderCreateRequest;
import com.allocator.mediaservice.dto.FolderResponse;
import com.allocator.mediaservice.mapper.FolderMapper;
import com.allocator.mediaservice.model.MediaFolder;
import com.allocator.mediaservice.repository.MediaFolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderService {

    private final MediaFolderRepository folderRepository;
    private final FolderMapper folderMapper;

    @Transactional
    public FolderResponse createFolder(FolderCreateRequest request, UUID userId) {
        // Check if folder name already exists in the same parent
        if (folderRepository.existsByNameAndParentFolder_Id(request.getName(), request.getParentFolderId())) {
            throw new IllegalArgumentException("Folder with name '" + request.getName() + 
                    "' already exists in this location");
        }

        MediaFolder parentFolder = null;
        if (request.getParentFolderId() != null) {
            parentFolder = folderRepository.findById(request.getParentFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
        }

        MediaFolder folder = MediaFolder.builder()
                .name(request.getName())
                .parentFolder(parentFolder)
                .createdBy(userId)
                .build();

        MediaFolder savedFolder = folderRepository.save(folder);
        log.info("Created folder: {} with ID: {}", savedFolder.getName(), savedFolder.getId());

        return folderMapper.toResponse(savedFolder);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFolderTree() {
        List<MediaFolder> rootFolders = folderRepository.findRootFolders();
        return folderMapper.toResponseList(rootFolders);
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolder(UUID id) {
        MediaFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + id));
        
        return folderMapper.toResponse(folder);
    }

    @Transactional
    public void deleteFolder(UUID id, UUID userId) {
        MediaFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + id));

        // Check if folder has files
        if (folderRepository.hasFiles(id)) {
            throw new IllegalStateException("Cannot delete folder: contains files");
        }

        // Check if folder has sub-folders
        if (folderRepository.hasChildFolders(id)) {
            throw new IllegalStateException("Cannot delete folder: contains sub-folders");
        }

        folderRepository.deleteById(id);
        log.info("Deleted folder: {} by user: {}", id, userId);
    }
}
