package com.allocator.contentservice.service;

import com.allocator.contentservice.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlugService {

    private final ContentRepository contentRepository;

    public String generateSlug(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }

        // Generate base slug
        String baseSlug = createBaseSlug(title);
        
        // Check if unique
        String uniqueSlug = ensureUniqueness(baseSlug);
        
        log.debug("Generated slug '{}' from title '{}'", uniqueSlug, title);
        return uniqueSlug;
    }

    private String createBaseSlug(String title) {
        // Normalize to NFD and remove diacritics
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        
        // Remove non-ASCII characters and convert to lowercase
        String slug = normalized.replaceAll("[^\\p{ASCII}]", "")
                           .toLowerCase()
                           .trim();
        
        // Replace spaces and special characters with hyphens
        slug = slug.replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-")
                   .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
        
        if (slug.isEmpty()) {
            // Fallback to UUID if slug becomes empty
            slug = "content-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        return slug;
    }

    private String ensureUniqueness(String baseSlug) {
        String uniqueSlug = baseSlug;
        int counter = 1;
        
        while (contentRepository.findBySlug(uniqueSlug).isPresent()) {
            uniqueSlug = baseSlug + "-" + counter;
            counter++;
            
            // Prevent infinite loop
            if (counter > 1000) {
                log.warn("Could not generate unique slug after 1000 attempts for base: {}", baseSlug);
                uniqueSlug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        
        return uniqueSlug;
    }

    public String generateSlugFromTitleAndId(String title, UUID id) {
        String baseSlug = createBaseSlug(title);
        return baseSlug + "-" + id.toString().substring(0, 8);
    }
}
