package com.allocator.contentservice.service;

import com.allocator.contentservice.model.DetectedIdentifier;
import com.allocator.contentservice.model.IdentifierType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ResourceDetectionService {

    // DOI: 10.XXXX/anything
    private static final Pattern DOI_PATTERN = Pattern.compile(
            "(?:doi:|https?://(?:dx\\.)?doi\\.org/)?(10\\.\\d{4,9}/[^\\s]+)", Pattern.CASE_INSENSITIVE);

    // ISBN-10 or ISBN-13 (with or without dashes)
    private static final Pattern ISBN_PATTERN = Pattern.compile(
            "(?:isbn[:\\s-]*)?((97[89][\\d-]{10,}|\\d[\\d-]{8,10}[\\dXx]))", Pattern.CASE_INSENSITIVE);

    // PMID: numeric, often prefixed
    private static final Pattern PMID_PATTERN = Pattern.compile(
            "(?:pmid[:\\s]*)?(\\d{7,8})(?!\\d)", Pattern.CASE_INSENSITIVE);

    // arXiv IDs: 2306.12345, 2306.12345v2, cs.AI/0601001
    private static final Pattern ARXIV_PATTERN = Pattern.compile(
            "(?:arxiv[:\\s/]*)?(\\d{4}\\.\\d{4,5}(?:v\\d+)?|[a-z-]+(?:\\.[A-Z]{2})?/\\d{7}(?:v\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?(?:youtube\\.com/watch\\?.*v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern VIMEO_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?vimeo\\.com/(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s]+", Pattern.CASE_INSENSITIVE);

    // Government TLD patterns
    private static final Pattern GOVT_PATTERN = Pattern.compile(
            "https?://[^\\s]*\\.(?:gov|mil|edu)(?:/|$|\\?)", Pattern.CASE_INSENSITIVE);

    // Podcast patterns (Spotify, Apple)
    private static final Pattern PODCAST_PATTERN = Pattern.compile(
            "https?://(?:open\\.spotify\\.com/episode|podcasts\\.apple\\.com|anchor\\.fm)[^\\s]+",
            Pattern.CASE_INSENSITIVE);

    // News domains
    private static final Pattern NEWS_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?(?:reuters|bbc|cnn|bloomberg|nytimes|theguardian|ft|wsj|apnews|npr|economist)[^\\s]+",
            Pattern.CASE_INSENSITIVE);

    public DetectedIdentifier detect(String rawInput) {
        String input = rawInput.trim();

        // 1. DOI
        Matcher doi = DOI_PATTERN.matcher(input);
        if (doi.find()) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.DOI)
                    .value(doi.group(1))
                    .normalizedInput(input)
                    .build();
        }

        // 2. arXiv (check before URL to catch arxiv.org URLs + bare IDs)
        if (input.contains("arxiv") || input.matches("\\d{4}\\.\\d{4,5}.*")) {
            Matcher arxiv = ARXIV_PATTERN.matcher(input);
            if (arxiv.find()) {
                return DetectedIdentifier.builder()
                        .type(IdentifierType.ARXIV)
                        .value(arxiv.group(1))
                        .normalizedInput(input)
                        .build();
            }
        }

        // 3. YouTube
        Matcher youtube = YOUTUBE_PATTERN.matcher(input);
        if (youtube.find()) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.YOUTUBE_URL)
                    .value(youtube.group(1))
                    .normalizedInput(normalizeUrl(input))
                    .build();
        }

        // 4. Vimeo
        Matcher vimeo = VIMEO_PATTERN.matcher(input);
        if (vimeo.find()) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.VIMEO_URL)
                    .value(vimeo.group(1))
                    .normalizedInput(normalizeUrl(input))
                    .build();
        }

        // 5. Podcast
        Matcher podcast = PODCAST_PATTERN.matcher(input);
        if (podcast.find()) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.PODCAST_URL)
                    .value(input)
                    .normalizedInput(normalizeUrl(input))
                    .build();
        }

        // 6. Government URL
        Matcher govt = GOVT_PATTERN.matcher(input);
        if (govt.find()) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.GOVERNMENT_URL)
                    .value(input)
                    .normalizedInput(normalizeUrl(input))
                    .build();
        }

        // 7. News URL
        Matcher news = NEWS_PATTERN.matcher(input);
        if (news.find()) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.NEWS_URL)
                    .value(input)
                    .normalizedInput(normalizeUrl(input))
                    .build();
        }

        // 8. ISBN (check after URL patterns to avoid false positives)
        String stripped = input.replaceAll("[^\\dXx]", "");
        if ((stripped.length() == 10 || stripped.length() == 13) && isValidIsbn(stripped)) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.ISBN)
                    .value(stripped)
                    .normalizedInput(input)
                    .build();
        }
        Matcher isbn = ISBN_PATTERN.matcher(input);
        if (isbn.find()) {
            String isbnVal = isbn.group(1).replaceAll("[^\\dXx]", "");
            if (isValidIsbn(isbnVal)) {
                return DetectedIdentifier.builder()
                        .type(IdentifierType.ISBN)
                        .value(isbnVal)
                        .normalizedInput(input)
                        .build();
            }
        }

        // 9. PMID (7-8 digit number)
        if (input.matches("\\d{7,8}")) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.PMID)
                    .value(input)
                    .normalizedInput(input)
                    .build();
        }
        Matcher pmid = PMID_PATTERN.matcher(input);
        if (pmid.find() && input.toLowerCase().contains("pmid")) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.PMID)
                    .value(pmid.group(1))
                    .normalizedInput(input)
                    .build();
        }

        // 10. Generic URL
        Matcher url = URL_PATTERN.matcher(input);
        if (url.find()) {
            return DetectedIdentifier.builder()
                    .type(IdentifierType.GENERIC_URL)
                    .value(input)
                    .normalizedInput(normalizeUrl(input))
                    .build();
        }

        return DetectedIdentifier.builder()
                .type(IdentifierType.UNKNOWN)
                .value(input)
                .normalizedInput(input)
                .build();
    }

    private boolean isValidIsbn(String s) {
        if (s.length() == 13) {
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                int d = Character.getNumericValue(s.charAt(i));
                sum += (i % 2 == 0) ? d : d * 3;
            }
            int check = (10 - (sum % 10)) % 10;
            return check == Character.getNumericValue(s.charAt(12));
        }
        if (s.length() == 10) {
            int sum = 0;
            for (int i = 0; i < 9; i++) sum += (10 - i) * Character.getNumericValue(s.charAt(i));
            char last = s.charAt(9);
            int checkVal = (last == 'X' || last == 'x') ? 10 : Character.getNumericValue(last);
            return (sum + checkVal) % 11 == 0;
        }
        return false;
    }

    private String normalizeUrl(String url) {
        url = url.trim();
        if (!url.startsWith("http")) url = "https://" + url;
        return url;
    }
}
