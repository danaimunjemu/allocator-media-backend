package com.allocator.searchservice.service;

import com.allocator.searchservice.dto.SearchResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchService {
    SearchResponse search(String q, String contentType, List<String> tags, String sector, Integer year, String brandId, Pageable pageable);
    List<String> suggest(String q);
    SearchResponse findRelated(String contentId, Pageable pageable);
    List<String> getTrendingTopics();
}
