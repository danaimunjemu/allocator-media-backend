package com.allocator.bookmarkservice.service;

import com.allocator.bookmarkservice.dto.ApiResponse;
import com.allocator.bookmarkservice.dto.SavedContentResponse;
import com.allocator.bookmarkservice.model.SavedContent;
import com.allocator.bookmarkservice.repository.SavedContentRepository;
import com.allocator.contentservice.mapper.ContentMapper;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedContentService {

    private final SavedContentRepository savedContentRepository;
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;

    @Transactional
    public void save(UUID contentId, UUID userId) {
        if (savedContentRepository.existsByUserIdAndContentId(userId, contentId)) return;

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        SavedContent saved = SavedContent.builder()
                .userId(userId)
                .contentId(contentId)
                .brandId(content.getBrandId())
                .build();
        savedContentRepository.save(saved);
        log.info("User {} saved content {}", userId, contentId);
    }

    @Transactional
    public void unsave(UUID contentId, UUID userId) {
        savedContentRepository.deleteByUserIdAndContentId(userId, contentId);
        log.info("User {} unsaved content {}", userId, contentId);
    }

    public boolean isSaved(UUID contentId, UUID userId) {
        return savedContentRepository.existsByUserIdAndContentId(userId, contentId);
    }

    public ApiResponse.PageResponse<SavedContentResponse> listSaved(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SavedContent> savedPage = savedContentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<UUID> contentIds = savedPage.getContent().stream()
                .map(SavedContent::getContentId)
                .collect(Collectors.toList());
        Map<UUID, Content> contentById = contentRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(Content::getId, c -> c));

        List<SavedContentResponse> responses = savedPage.getContent().stream()
                // A saved item whose content has since been deleted has
                // nothing left to render — quietly drop it rather than error.
                .filter(sc -> contentById.containsKey(sc.getContentId()))
                .map(sc -> SavedContentResponse.builder()
                        .content(contentMapper.toResponseEnriched(contentById.get(sc.getContentId())))
                        .savedAt(sc.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        Page<SavedContentResponse> resultPage = new PageImpl<>(responses, pageable, savedPage.getTotalElements());
        return ApiResponse.PageResponse.from(resultPage);
    }
}
