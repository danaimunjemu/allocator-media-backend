package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.ReferenceRequest;
import com.allocator.contentservice.model.Reference;
import com.allocator.contentservice.model.ReferenceSourceType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates a 0–100 metadata completeness score for a reference.
 * Required fields differ by source type; each missing required field deducts from the score.
 */
@Service
public class MetadataCompletenessScorer {

    public record CompletenessResult(int score, List<String> missingFields) {}

    public CompletenessResult score(ReferenceRequest req) {
        List<String> required = getRequiredFields(req.getSourceType());
        if (required.isEmpty()) return new CompletenessResult(100, List.of());

        List<String> missing = new ArrayList<>();
        for (String field : required) {
            if (isMissing(req, field)) missing.add(field);
        }

        int score = (int) Math.round(100.0 * (required.size() - missing.size()) / required.size());
        return new CompletenessResult(score, missing);
    }

    public CompletenessResult score(Reference ref) {
        List<String> required = getRequiredFields(ref.getSourceType());
        if (required.isEmpty()) return new CompletenessResult(100, List.of());

        List<String> missing = new ArrayList<>();
        for (String field : required) {
            if (isMissingEntity(ref, field)) missing.add(field);
        }

        int score = (int) Math.round(100.0 * (required.size() - missing.size()) / required.size());
        return new CompletenessResult(score, missing);
    }

    private List<String> getRequiredFields(ReferenceSourceType type) {
        if (type == null) return List.of("title");
        return switch (type) {
            case JOURNAL_ARTICLE -> List.of("title", "contributors", "journalName", "pubYear", "doi");
            case BOOK            -> List.of("title", "contributors", "publisher", "pubYear", "isbn");
            case BOOK_CHAPTER    -> List.of("title", "contributors", "journalName", "publisher", "pubYear", "pageStart");
            case WEBPAGE         -> List.of("title", "url", "accessDate");
            case ONLINE_VIDEO    -> List.of("title", "url", "channelName", "pubYear");
            case NEWSPAPER_ARTICLE -> List.of("title", "contributors", "journalName", "pubYear", "url");
            case REPORT          -> List.of("title", "contributors", "publisher", "pubYear");
            case BLOG_POST       -> List.of("title", "contributors", "websiteName", "pubYear", "url");
            case PODCAST         -> List.of("title", "channelName", "pubYear", "url");
            case CONFERENCE_PAPER -> List.of("title", "contributors", "conferenceTitle", "pubYear");
            case DISSERTATION, SOFTWARE -> List.of("title", "contributors", "institution", "pubYear");
            case GOVERNMENT_DOCUMENT -> List.of("title", "publisher", "pubYear", "url");
            default              -> List.of("title", "pubYear", "url");
        };
    }

    private boolean isMissing(ReferenceRequest r, String field) {
        return switch (field) {
            case "title"          -> blank(r.getTitle());
            case "contributors"   -> blank(r.getContributorsJson()) || r.getContributorsJson().equals("[]");
            case "journalName"    -> blank(r.getJournalName());
            case "pubYear"        -> r.getPubYear() == null;
            case "doi"            -> blank(r.getDoi());
            case "publisher"      -> blank(r.getPublisher());
            case "isbn"           -> blank(r.getIsbn());
            case "pageStart"      -> blank(r.getPageStart());
            case "url"            -> blank(r.getUrl());
            case "accessDate"     -> blank(r.getAccessDate());
            case "channelName"    -> blank(r.getChannelName());
            case "websiteName"    -> blank(r.getWebsiteName());
            case "conferenceTitle"-> blank(r.getConferenceTitle());
            case "institution"    -> blank(r.getInstitution());
            default               -> false;
        };
    }

    private boolean isMissingEntity(Reference r, String field) {
        return switch (field) {
            case "title"          -> blank(r.getTitle());
            case "contributors"   -> blank(r.getContributorsJson()) || "[]".equals(r.getContributorsJson());
            case "journalName"    -> blank(r.getJournalName());
            case "pubYear"        -> r.getPubYear() == null;
            case "doi"            -> blank(r.getDoi());
            case "publisher"      -> blank(r.getPublisher());
            case "isbn"           -> blank(r.getIsbn());
            case "pageStart"      -> blank(r.getPageStart());
            case "url"            -> blank(r.getUrl());
            case "accessDate"     -> blank(r.getAccessDate());
            case "channelName"    -> blank(r.getChannelName());
            case "websiteName"    -> blank(r.getWebsiteName());
            case "conferenceTitle"-> blank(r.getConferenceTitle());
            case "institution"    -> blank(r.getInstitution());
            default               -> false;
        };
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
