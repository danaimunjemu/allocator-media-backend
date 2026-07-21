package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
    Optional<Category> findBySlug(String slug);
}
