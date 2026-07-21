package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.dto.AuthorRequest;
import com.allocator.contentservice.dto.AuthorResponse;
import com.allocator.contentservice.service.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> getAllAuthors() {
        return ResponseEntity.ok(ApiResponse.success(authorService.getAllAuthors(), null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthorResponse>> getAuthor(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(authorService.getAuthor(id), null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthor(@Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authorService.createAuthor(request), "Author created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthor(@PathVariable UUID id, @Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authorService.updateAuthor(id, request), "Author updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAuthor(@PathVariable UUID id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Author deleted successfully"));
    }

    @PostMapping("/find-or-create")
    public ResponseEntity<ApiResponse<AuthorResponse>> findOrCreate(@RequestBody AuthorRequest request) {
        UUID userId = null;
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            try {
                userId = UUID.fromString(request.getUserId());
            } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(ApiResponse.success(
            authorService.findOrCreate(request.getName(), request.getEmail(), request.getAvatarUrl(), request.getRole(), userId),
            "Author resolved"
        ));
    }
}
