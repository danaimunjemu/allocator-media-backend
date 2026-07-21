package com.allocator.searchservice.controller;

import com.allocator.searchservice.dto.SearchResponse;
import com.allocator.searchservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) List<String> tag,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        SearchResponse response = searchService.search(q, contentType, tag, sector, year, brandId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(@RequestParam String q) {
        return ResponseEntity.ok(searchService.suggest(q));
    }

    @GetMapping("/related/{contentId}")
    public ResponseEntity<SearchResponse> findRelated(
            @PathVariable String contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ResponseEntity.ok(searchService.findRelated(contentId, pageable));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<String>> getTrending() {
        return ResponseEntity.ok(searchService.getTrendingTopics());
    }
}
