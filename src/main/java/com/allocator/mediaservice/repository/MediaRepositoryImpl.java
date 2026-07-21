package com.allocator.mediaservice.repository;

import com.allocator.mediaservice.dto.MediaFilter;
import com.allocator.mediaservice.model.Media;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MediaRepositoryImpl implements MediaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Media> findAll(MediaFilter filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Media> query = cb.createQuery(Media.class);
        Root<Media> root = query.from(Media.class);

        List<Predicate> predicates = new ArrayList<>();

        // Add filter predicates
        if (filter.getBrandId() != null) {
            predicates.add(cb.equal(root.get("brandId"), filter.getBrandId()));
        }

        if (filter.getContentType() != null) {
            predicates.add(cb.equal(root.get("contentType"), filter.getContentType()));
        }

        if (filter.getUploadedBy() != null) {
            predicates.add(cb.equal(root.get("uploadedBy"), filter.getUploadedBy()));
        }

        if (filter.getIsPublic() != null) {
            predicates.add(cb.equal(root.get("isPublic"), filter.getIsPublic()));
        }

        if (filter.getIsArchived() != null) {
            predicates.add(cb.equal(root.get("isArchived"), filter.getIsArchived()));
        }

        if (filter.getProcessingStatus() != null) {
            predicates.add(cb.equal(root.get("processingStatus"), filter.getProcessingStatus()));
        }

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            String keywordPattern = "%" + filter.getKeyword().toLowerCase() + "%";
            Predicate fileNamePredicate = cb.like(cb.lower(root.get("fileName")), keywordPattern);
            Predicate originalNamePredicate = cb.like(cb.lower(root.get("originalName")), keywordPattern);
            predicates.add(cb.or(fileNamePredicate, originalNamePredicate));
        }

        if (filter.getUploadedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getUploadedAfter()));
        }

        if (filter.getUploadedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getUploadedBefore()));
        }

        if (filter.getHasExpired() != null) {
            if (filter.getHasExpired()) {
                predicates.add(cb.lessThan(root.get("expiresAt"), Instant.now()));
            } else {
                predicates.add(cb.or(
                        cb.isNull(root.get("expiresAt")),
                        cb.greaterThan(root.get("expiresAt"), Instant.now())
                ));
            }
        } else {
            // Default: show non-expired media
            predicates.add(cb.or(
                    cb.isNull(root.get("expiresAt")),
                    cb.greaterThan(root.get("expiresAt"), Instant.now())
            ));
        }

        if (filter.getIds() != null && !filter.getIds().isEmpty()) {
            predicates.add(root.get("id").in(filter.getIds()));
        }

        // Apply all predicates
        query.where(predicates.toArray(new Predicate[0]));

        // Execute query with pagination
        TypedQuery<Media> typedQuery = entityManager.createQuery(query);
        
        int totalElements = typedQuery.getResultList().size();
        
        // Apply pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Media> results = typedQuery.getResultList();

        return new PageImpl<>(results, pageable, totalElements);
    }
}
