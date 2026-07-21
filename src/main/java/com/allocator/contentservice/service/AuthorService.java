package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.AuthorRequest;
import com.allocator.contentservice.dto.AuthorResponse;
import com.allocator.contentservice.model.Author;
import com.allocator.contentservice.repository.AuthorRepository;
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
public class AuthorService {

    private final AuthorRepository authorRepository;

    @Transactional(readOnly = true)
    public List<AuthorResponse> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AuthorResponse getAuthor(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Author not found: " + id));
        return mapToResponse(author);
    }

    @Transactional
    public AuthorResponse createAuthor(AuthorRequest request) {
        UUID parsedUserId = parseUserId(request.getUserId());
        Author author = Author.builder()
                .name(request.getName())
                .bio(request.getBio())
                .avatarUrl(request.getAvatarUrl())
                .email(request.getEmail())
                .role(request.getRole())
                .contactInfo(request.getContactInfo())
                .socialLinks(request.getSocialLinks())
                .userId(parsedUserId)
                .build();

        Author savedAuthor = authorRepository.save(author);
        log.info("Created author: {}", savedAuthor.getId());
        return mapToResponse(savedAuthor);
    }

    @Transactional
    public AuthorResponse updateAuthor(UUID id, AuthorRequest request) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Author not found: " + id));

        author.setName(request.getName());
        author.setBio(request.getBio());
        author.setAvatarUrl(request.getAvatarUrl());
        author.setEmail(request.getEmail());
        author.setRole(request.getRole());
        author.setContactInfo(request.getContactInfo());
        author.setSocialLinks(request.getSocialLinks());
        if (request.getUserId() != null) {
            author.setUserId(parseUserId(request.getUserId()));
        }

        Author savedAuthor = authorRepository.save(author);
        log.info("Updated author: {}", savedAuthor.getId());
        return mapToResponse(savedAuthor);
    }

    @Transactional
    public void deleteAuthor(UUID id) {
        if (!authorRepository.existsById(id)) {
            throw new IllegalArgumentException("Author not found: " + id);
        }
        authorRepository.deleteById(id);
        log.info("Deleted author: {}", id);
    }

    @Transactional
    public AuthorResponse findOrCreate(String name, String email, String avatarUrl, String role, UUID userId) {
        // 1. Look up by userId first (most reliable — won't create duplicates for the same user)
        if (userId != null) {
            java.util.Optional<Author> byUserId = authorRepository.findByUserId(userId);
            if (byUserId.isPresent()) {
                Author author = byUserId.get();
                // Keep profile in sync with latest user info
                boolean dirty = false;
                if (name != null && !name.isBlank() && !name.equals(author.getName())) {
                    author.setName(name);
                    dirty = true;
                }
                if (avatarUrl != null && !avatarUrl.equals(author.getAvatarUrl())) {
                    author.setAvatarUrl(avatarUrl);
                    dirty = true;
                }
                if (email != null && !email.isBlank() && !email.equals(author.getEmail())) {
                    author.setEmail(email);
                    dirty = true;
                }
                if (dirty) authorRepository.save(author);
                return mapToResponse(author);
            }
        }

        // 2. Fall back to email lookup
        if (email != null && !email.isBlank()) {
            java.util.Optional<Author> byEmail = authorRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                Author author = byEmail.get();
                // Link this author to the user account if not yet linked
                if (userId != null && author.getUserId() == null) {
                    author.setUserId(userId);
                    authorRepository.save(author);
                }
                return mapToResponse(author);
            }
        }

        // 3. Create a new author profile
        Author author = Author.builder()
                .name(name != null && !name.isBlank() ? name : "Unknown")
                .email(email)
                .avatarUrl(avatarUrl)
                .role(role)
                .userId(userId)
                .build();
        return mapToResponse(authorRepository.save(author));
    }

    private UUID parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank()) return null;
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid userId format: {}", userIdStr);
            return null;
        }
    }

    private AuthorResponse mapToResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .userId(author.getUserId())
                .name(author.getName())
                .bio(author.getBio())
                .avatarUrl(author.getAvatarUrl())
                .email(author.getEmail())
                .role(author.getRole())
                .contactInfo(author.getContactInfo())
                .socialLinks(author.getSocialLinks())
                .createdAt(author.getCreatedAt())
                .updatedAt(author.getUpdatedAt())
                .build();
    }
}
