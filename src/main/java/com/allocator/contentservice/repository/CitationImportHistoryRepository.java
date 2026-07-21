package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.CitationImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CitationImportHistoryRepository extends JpaRepository<CitationImportHistory, UUID> {
    List<CitationImportHistory> findByCreatedByUserIdOrderByCreatedAtDesc(UUID userId);
    List<CitationImportHistory> findTop50ByOrderByCreatedAtDesc();
}
