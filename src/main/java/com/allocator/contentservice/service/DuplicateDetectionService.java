package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.ReferenceRequest;
import com.allocator.contentservice.model.Reference;
import com.allocator.contentservice.repository.ReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final ReferenceRepository referenceRepository;

    public record DuplicateResult(boolean isDuplicate, Reference existing, String matchedOn) {}

    @Transactional(readOnly = true)
    public DuplicateResult detect(ReferenceRequest request) {
        // Exact DOI match
        if (notBlank(request.getDoi())) {
            Optional<Reference> byDoi = referenceRepository.findFirstByDoi(normalizeDoi(request.getDoi()));
            if (byDoi.isPresent()) return new DuplicateResult(true, byDoi.get(), "DOI");
        }

        // Exact ISBN match
        if (notBlank(request.getIsbn())) {
            Optional<Reference> byIsbn = referenceRepository.findFirstByIsbn(normalizeIsbn(request.getIsbn()));
            if (byIsbn.isPresent()) return new DuplicateResult(true, byIsbn.get(), "ISBN");
        }

        // Exact URL match (normalized)
        if (notBlank(request.getUrl())) {
            Optional<Reference> byUrl = referenceRepository.findFirstByUrl(normalizeUrl(request.getUrl()));
            if (byUrl.isPresent()) return new DuplicateResult(true, byUrl.get(), "URL");
        }

        // Fuzzy title match (Levenshtein distance <= 3 on normalized title)
        if (notBlank(request.getTitle())) {
            var candidates = referenceRepository.findByNormalizedTitle(request.getTitle());
            for (Reference candidate : candidates) {
                if (levenshtein(normalize(request.getTitle()), normalize(candidate.getTitle())) <= 3) {
                    return new DuplicateResult(true, candidate, "title");
                }
            }
        }

        return new DuplicateResult(false, null, null);
    }

    private String normalizeDoi(String doi) {
        if (doi == null) return null;
        doi = doi.trim().toLowerCase();
        if (doi.startsWith("https://doi.org/")) doi = doi.substring("https://doi.org/".length());
        if (doi.startsWith("http://doi.org/"))  doi = doi.substring("http://doi.org/".length());
        if (doi.startsWith("doi:"))             doi = doi.substring(4);
        return doi;
    }

    private String normalizeIsbn(String isbn) {
        return isbn == null ? null : isbn.replaceAll("[^0-9X]", "").toLowerCase();
    }

    private String normalizeUrl(String url) {
        if (url == null) return null;
        url = url.trim().toLowerCase();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url.replaceFirst("^https://", "").replaceFirst("^http://", "").replaceFirst("^www\\.", "");
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }
}
