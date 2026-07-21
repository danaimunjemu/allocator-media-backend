package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.dto.ContentRequest;
import com.allocator.contentservice.dto.ContentResponse;
import com.allocator.contentservice.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {

    private final ContentService contentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContentResponse>> createArticle(
            @Valid @RequestBody ContentRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Creating article by user: {} with roles: {}", userId, userRoles);
        ContentResponse response = contentService.createContent(request, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Article created successfully"));
    }
}

