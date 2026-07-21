package com.allocator.searchservice.service;

import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentStatus;
import com.allocator.contentservice.model.ContentType;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.searchservice.dto.ContentIndexDto;
import com.allocator.searchservice.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ContentRepository contentRepository;

    @Override
    public SearchResponse search(String q, String contentType, List<String> tags, String sector, Integer year, String brandId, Pageable pageable) {
        Specification<Content> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only search PUBLISHED content
            predicates.add(cb.equal(root.get("status"), ContentStatus.PUBLISHED));

            if (q != null && !q.trim().isEmpty()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("summary")), pattern),
                        cb.like(cb.lower(root.get("body")), pattern)
                ));
            }

            if (contentType != null && !contentType.trim().isEmpty()) {
                try {
                    predicates.add(cb.equal(root.get("contentType"), ContentType.valueOf(contentType.toUpperCase())));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid content type filter: {}", contentType);
                }
            }

            if (brandId != null && !brandId.trim().isEmpty()) {
                try {
                    predicates.add(cb.equal(root.get("brandId"), UUID.fromString(brandId)));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid brand id filter: {}", brandId);
                }
            }

            if (tags != null && !tags.isEmpty()) {
                Join<Content, String> tagsJoin = root.join("tags");
                predicates.add(tagsJoin.in(tags));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Content> contentPage = contentRepository.findAll(spec, pageable);

        // Memory filtering for sector/year (if metadata is not null) to ensure absolute cross-database compatibility
        List<ContentIndexDto> results = contentPage.getContent().stream()
                .filter(content -> {
                    if (sector != null && !sector.trim().isEmpty()) {
                        if (content.getMetadata() == null || !sector.equalsIgnoreCase(String.valueOf(content.getMetadata().get("sector")))) {
                            return false;
                        }
                    }
                    if (year != null) {
                        if (content.getMetadata() == null || !year.toString().equals(String.valueOf(content.getMetadata().get("year")))) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(this::toDto)
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .totalResults(contentPage.getTotalElements())
                .results(results)
                .build();
    }

    @Override
    public List<String> suggest(String q) {
        if (q == null || q.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Pageable limit = org.springframework.data.domain.PageRequest.of(0, 10);
        return contentRepository.searchByKeyword(q, limit).getContent().stream()
                .map(Content::getTitle)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public SearchResponse findRelated(String contentId, Pageable pageable) {
        try {
            UUID id = UUID.fromString(contentId);
            Content content = contentRepository.findById(id).orElse(null);
            if (content == null) {
                return SearchResponse.builder().totalResults(0).results(Collections.emptyList()).build();
            }

            Specification<Content> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("status"), ContentStatus.PUBLISHED));
                predicates.add(cb.notEqual(root.get("id"), id));
                predicates.add(cb.equal(root.get("contentType"), content.getContentType()));
                predicates.add(cb.equal(root.get("brandId"), content.getBrandId()));

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<Content> relatedPage = contentRepository.findAll(spec, pageable);
            List<ContentIndexDto> results = relatedPage.getContent().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            return SearchResponse.builder()
                    .totalResults(relatedPage.getTotalElements())
                    .results(results)
                    .build();
        } catch (Exception e) {
            log.error("Error finding related content for {}", contentId, e);
            return SearchResponse.builder().totalResults(0).results(Collections.emptyList()).build();
        }
    }

    @Override
    public List<String> getTrendingTopics() {
        // Return a popular tags list
        return List.of("markets", "africa", "equities", "mining", "investing", "tech");
    }

    private ContentIndexDto toDto(Content content) {
        String authorId = content.getAuthors() != null && !content.getAuthors().isEmpty() ? content.getAuthors().get(0).getId().toString() : null;
        
        // Extract optional metadata fields
        String sector = null;
        Integer year = null;
        if (content.getMetadata() != null) {
            sector = (String) content.getMetadata().get("sector");
            try {
                Object yrObj = content.getMetadata().get("year");
                if (yrObj != null) {
                    year = Integer.parseInt(yrObj.toString());
                }
            } catch (Exception ignored) {}
        }

        return ContentIndexDto.builder()
                .contentId(content.getId().toString())
                .id(content.getId().toString())
                .brandId(content.getBrandId().toString())
                .title(content.getTitle())
                .summary(content.getSummary())
                .body(content.getBody())
                .contentType(content.getContentType().name())
                .slug(content.getSlug())
                .tags(content.getTags())
                .topics(content.getTags()) // mapped to tags as topics for frontend
                .sector(sector)
                .year(year)
                .publishedAt(content.getPublishedAt())
                .authorId(authorId)
                .build();
    }
}
