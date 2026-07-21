package com.allocator.bookmarkservice.repository;

import com.allocator.bookmarkservice.model.SavedContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedContentRepository extends JpaRepository<SavedContent, UUID> {

    Page<SavedContent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<SavedContent> findByUserIdAndContentId(UUID userId, UUID contentId);

    boolean existsByUserIdAndContentId(UUID userId, UUID contentId);

    long countByUserId(UUID userId);

    void deleteByUserIdAndContentId(UUID userId, UUID contentId);
}
