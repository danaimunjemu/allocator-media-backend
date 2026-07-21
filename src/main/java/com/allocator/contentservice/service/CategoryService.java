package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.CategoryRequest;
import com.allocator.contentservice.dto.CategoryResponse;
import com.allocator.contentservice.model.Category;
import com.allocator.contentservice.repository.CategoryRepository;
import com.allocator.contentservice.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ContentRepository contentRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = resolveSlug(request.getName(), request.getSlug());

        if (categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("A category with slug '" + slug + "' already exists");
        }
        if (categoryRepository.existsByName(request.getName().trim())) {
            throw new IllegalArgumentException("A category named '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .color(request.getColor())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Created category: {} ({})", saved.getName(), saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        String slug = resolveSlug(request.getName(), request.getSlug());

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new IllegalArgumentException("A category with slug '" + slug + "' already exists");
        }
        if (categoryRepository.existsByNameAndIdNot(request.getName().trim(), id)) {
            throw new IllegalArgumentException("A category named '" + request.getName() + "' already exists");
        }

        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());

        Category saved = categoryRepository.save(category);
        log.info("Updated category: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Deleted category: {}", id);
    }

    private String resolveSlug(String name, String requestedSlug) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            return requestedSlug.toLowerCase().trim().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        }
        return name.toLowerCase().trim().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .color(category.getColor())
                .contentCount(contentRepository.countByCategoryId(category.getId()))
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
