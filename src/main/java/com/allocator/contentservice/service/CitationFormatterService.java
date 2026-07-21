package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.ReferenceRequest;
import com.allocator.contentservice.model.CitationStyle;
import com.allocator.contentservice.model.ReferenceSourceType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitationFormatterService {

    private final ObjectMapper objectMapper;

    public String format(ReferenceRequest req) {
        if (req == null || req.getTitle() == null) return "";
        CitationStyle style = req.getCitationStyle() != null ? req.getCitationStyle() : CitationStyle.APA_7;
        return switch (style) {
            case APA_7      -> formatApa7(req);
            case MLA_9      -> formatMla9(req);
            case HARVARD    -> formatHarvard(req);
            case CHICAGO_17 -> formatChicago17(req);
            case IEEE       -> formatIeee(req);
            case VANCOUVER  -> formatVancouver(req);
        };
    }

    // ── APA 7th ────────────────────────────────────────────────────────────────

    private String formatApa7(ReferenceRequest r) {
        ReferenceSourceType type = r.getSourceType();
        if (type == null) return buildApaGeneric(r);
        return switch (type) {
            case JOURNAL_ARTICLE -> buildApaJournal(r);
            case WEBPAGE, BLOG_POST, NEWSPAPER_ARTICLE -> buildApaWebpage(r);
            case BOOK, BOOK_CHAPTER -> buildApaBook(r);
            case REPORT -> buildApaReport(r);
            case ONLINE_VIDEO -> buildApaVideo(r);
            case CONFERENCE_PAPER -> buildApaConferencePaper(r);
            case DISSERTATION, SOFTWARE -> buildApaThesis(r);
            default -> buildApaGeneric(r);
        };
    }

    private String buildApaJournal(ReferenceRequest r) {
        // Author, A. A. (Year). Title. Journal, volume(issue), pages. doi
        StringBuilder sb = new StringBuilder();
        appendAuthorsApa(sb, r.getContributorsJson());
        appendYear(sb, r.getPubYear());
        sb.append(r.getTitle()).append(". ");
        if (r.getJournalName() != null) sb.append(italic(r.getJournalName())).append(", ");
        if (r.getVolumeStart() != null) sb.append(italic(r.getVolumeStart()));
        if (r.getIssue() != null) sb.append("(").append(r.getIssue()).append(")");
        appendPages(sb, r.getPageStart(), r.getPageEnd());
        appendDoi(sb, r.getDoi());
        return sb.toString().trim();
    }

    private String buildApaWebpage(ReferenceRequest r) {
        // Author. (Year, Month Day). Title. Website. URL
        StringBuilder sb = new StringBuilder();
        appendAuthorsApa(sb, r.getContributorsJson());
        appendDate(sb, r.getPubYear(), r.getPubMonth(), r.getPubDay());
        sb.append(italic(r.getTitle())).append(". ");
        if (r.getWebsiteName() != null) sb.append(r.getWebsiteName()).append(". ");
        if (r.getUrl() != null) sb.append(r.getUrl());
        return sb.toString().trim();
    }

    private String buildApaBook(ReferenceRequest r) {
        // Author. (Year). Title (Ed. ed.). Publisher.
        StringBuilder sb = new StringBuilder();
        appendAuthorsApa(sb, r.getContributorsJson());
        appendYear(sb, r.getPubYear());
        sb.append(italic(r.getTitle()));
        if (r.getEdition() != null) sb.append(" (").append(r.getEdition()).append(" ed.)");
        sb.append(". ");
        if (r.getPublisher() != null) sb.append(r.getPublisher()).append(".");
        return sb.toString().trim();
    }

    private String buildApaReport(ReferenceRequest r) {
        StringBuilder sb = new StringBuilder();
        appendAuthorsApa(sb, r.getContributorsJson());
        appendYear(sb, r.getPubYear());
        sb.append(italic(r.getTitle())).append(". ");
        if (r.getPublisher() != null) sb.append(r.getPublisher()).append(". ");
        if (r.getUrl() != null) sb.append(r.getUrl());
        return sb.toString().trim();
    }

    private String buildApaVideo(ReferenceRequest r) {
        // Author. (Year, Month Day). Title [Video]. Platform. URL
        StringBuilder sb = new StringBuilder();
        appendAuthorsApa(sb, r.getContributorsJson());
        appendDate(sb, r.getPubYear(), r.getPubMonth(), r.getPubDay());
        sb.append(italic(r.getTitle())).append(" [Video]. ");
        if (r.getWebsiteName() != null) sb.append(r.getWebsiteName()).append(". ");
        if (r.getUrl() != null) sb.append(r.getUrl());
        return sb.toString().trim();
    }

    private String buildApaConferencePaper(ReferenceRequest r) {
        // Author, A. A. (Year). Title. In Conference Name (pp. X–Y). Publisher. doi
        StringBuilder sb = new StringBuilder();
        appendAuthorsApa(sb, r.getContributorsJson());
        appendYear(sb, r.getPubYear());
        sb.append(r.getTitle()).append(". ");
        if (r.getConferenceTitle() != null) sb.append("In ").append(italic(r.getConferenceTitle()));
        appendPages(sb, r.getPageStart(), r.getPageEnd());
        sb.append(". ");
        if (r.getPublisher() != null) sb.append(r.getPublisher()).append(". ");
        appendDoi(sb, r.getDoi());
        return sb.toString().trim();
    }

    private String buildApaThesis(ReferenceRequest r) {
        // Author, A. A. (Year). Title [Doctoral dissertation, Institution]. Repository/URL
        StringBuilder sb = new StringBuilder();
        appendAuthorsApa(sb, r.getContributorsJson());
        appendYear(sb, r.getPubYear());
        sb.append(italic(r.getTitle()));
        String degreeLabel = r.getDegree() != null ? r.getDegree() : "Doctoral dissertation";
        sb.append(" [").append(degreeLabel);
        if (r.getInstitution() != null) sb.append(", ").append(r.getInstitution());
        sb.append("]. ");
        if (r.getUrl() != null) sb.append(r.getUrl());
        return sb.toString().trim();
    }

    private String buildApaGeneric(ReferenceRequest r) {
        StringBuilder sb = new StringBuilder();
        appendAuthorsApa(sb, r.getContributorsJson());
        appendYear(sb, r.getPubYear());
        sb.append(r.getTitle()).append(". ");
        if (r.getUrl() != null) sb.append(r.getUrl());
        return sb.toString().trim();
    }

    // ── MLA 9th ────────────────────────────────────────────────────────────────

    private String formatMla9(ReferenceRequest r) {
        ReferenceSourceType type = r.getSourceType();
        StringBuilder sb = new StringBuilder();
        appendAuthorsMla(sb, r.getContributorsJson());
        sb.append('"').append(r.getTitle()).append('.').append('"').append(" ");
        if (type == ReferenceSourceType.JOURNAL_ARTICLE) {
            if (r.getJournalName() != null) sb.append(italic(r.getJournalName())).append(", ");
            if (r.getVolumeStart() != null) sb.append("vol. ").append(r.getVolumeStart()).append(", ");
            if (r.getIssue() != null) sb.append("no. ").append(r.getIssue()).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(", ");
            appendPagesMla(sb, r.getPageStart(), r.getPageEnd());
            if (r.getDoi() != null) sb.append("doi:").append(r.getDoi()).append(".");
        } else if (type == ReferenceSourceType.BOOK || type == ReferenceSourceType.BOOK_CHAPTER) {
            if (r.getPublisher() != null) sb.append(r.getPublisher()).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else if (type == ReferenceSourceType.CONFERENCE_PAPER) {
            if (r.getConferenceTitle() != null) sb.append(italic("Proceedings of " + r.getConferenceTitle())).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(", ");
            appendPagesMla(sb, r.getPageStart(), r.getPageEnd());
        } else if (type == ReferenceSourceType.DISSERTATION) {
            String deg = r.getDegree() != null ? r.getDegree() : "Dissertation";
            sb.append(deg).append(", ");
            if (r.getInstitution() != null) sb.append(r.getInstitution()).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else {
            if (r.getWebsiteName() != null) sb.append(italic(r.getWebsiteName())).append(", ");
            appendDate(sb, r.getPubYear(), r.getPubMonth(), r.getPubDay());
            if (r.getUrl() != null) sb.append(r.getUrl()).append(".");
        }
        return sb.toString().trim();
    }

    // ── Harvard ────────────────────────────────────────────────────────────────

    private String formatHarvard(ReferenceRequest r) {
        StringBuilder sb = new StringBuilder();
        appendAuthorsHarvard(sb, r.getContributorsJson());
        if (r.getPubYear() != null) sb.append(" (").append(r.getPubYear()).append(") ");
        sb.append("'").append(r.getTitle()).append("'");
        ReferenceSourceType type = r.getSourceType();
        if (type == ReferenceSourceType.JOURNAL_ARTICLE) {
            if (r.getJournalName() != null) sb.append(", ").append(italic(r.getJournalName()));
            if (r.getVolumeStart() != null) sb.append(", vol. ").append(r.getVolumeStart());
            if (r.getIssue() != null) sb.append(", no. ").append(r.getIssue());
            appendPages(sb, r.getPageStart(), r.getPageEnd());
            appendDoi(sb, r.getDoi());
        } else if (type == ReferenceSourceType.CONFERENCE_PAPER) {
            if (r.getConferenceTitle() != null) sb.append(", in ").append(italic(r.getConferenceTitle()));
            appendPages(sb, r.getPageStart(), r.getPageEnd());
            if (r.getPubYear() != null) sb.append(", ").append(r.getPubYear());
        } else if (type == ReferenceSourceType.DISSERTATION) {
            String deg = r.getDegree() != null ? r.getDegree() : "Doctoral dissertation";
            sb.append(", ").append(deg);
            if (r.getInstitution() != null) sb.append(", ").append(r.getInstitution());
        } else {
            if (r.getPublisher() != null) sb.append(", ").append(r.getPublisher());
            if (r.getUrl() != null) sb.append(". Available at: ").append(r.getUrl());
        }
        sb.append(".");
        return sb.toString().trim();
    }

    // ── Chicago 17th ───────────────────────────────────────────────────────────

    private String formatChicago17(ReferenceRequest r) {
        StringBuilder sb = new StringBuilder();
        appendAuthorsChicago(sb, r.getContributorsJson());
        sb.append('"').append(r.getTitle()).append('.').append('"').append(" ");
        ReferenceSourceType type = r.getSourceType();
        if (type == ReferenceSourceType.JOURNAL_ARTICLE) {
            if (r.getJournalName() != null) sb.append(italic(r.getJournalName())).append(" ");
            if (r.getVolumeStart() != null) sb.append(r.getVolumeStart());
            if (r.getIssue() != null) sb.append(", no. ").append(r.getIssue());
            if (r.getPubYear() != null) sb.append(" (").append(r.getPubYear()).append(")");
            appendPages(sb, r.getPageStart(), r.getPageEnd());
            appendDoi(sb, r.getDoi());
        } else if (type == ReferenceSourceType.CONFERENCE_PAPER) {
            if (r.getConferenceTitle() != null) sb.append("In ").append(italic(r.getConferenceTitle())).append(", ");
            appendPagesMla(sb, r.getPageStart(), r.getPageEnd());
            if (r.getPublisher() != null) sb.append(r.getPublisher()).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else if (type == ReferenceSourceType.DISSERTATION) {
            String deg = r.getDegree() != null ? r.getDegree() : "PhD diss.";
            sb.append(deg).append(", ");
            if (r.getInstitution() != null) sb.append(r.getInstitution()).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else {
            if (r.getPublisher() != null) sb.append(r.getPublisher()).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(". ");
            if (r.getUrl() != null) sb.append(r.getUrl()).append(".");
        }
        return sb.toString().trim();
    }

    // ── IEEE ───────────────────────────────────────────────────────────────────

    private String formatIeee(ReferenceRequest r) {
        // A. B. Author, "Title," Journal Name, vol. X, no. Y, pp. ZZ-ZZ, Mon. YYYY, doi: XX.
        StringBuilder sb = new StringBuilder();
        appendAuthorsIeee(sb, r.getContributorsJson());
        sb.append('"').append(r.getTitle()).append(',' ).append('"').append(" ");
        if (r.getSourceType() == ReferenceSourceType.JOURNAL_ARTICLE) {
            if (r.getJournalName() != null) sb.append(r.getJournalName()).append(", ");
            if (r.getVolumeStart() != null) sb.append("vol. ").append(r.getVolumeStart()).append(", ");
            if (r.getIssue() != null) sb.append("no. ").append(r.getIssue()).append(", ");
            if (r.getPageStart() != null) {
                sb.append("pp. ").append(r.getPageStart());
                if (r.getPageEnd() != null) sb.append("–").append(r.getPageEnd());
                sb.append(", ");
            }
            if (r.getPubYear() != null) {
                String month = r.getPubMonth() != null ? monthAbbrev(r.getPubMonth()) + ". " : "";
                sb.append(month).append(r.getPubYear());
            }
            if (r.getDoi() != null) sb.append(", doi: ").append(r.getDoi());
            sb.append(".");
        } else if (r.getSourceType() == ReferenceSourceType.BOOK || r.getSourceType() == ReferenceSourceType.BOOK_CHAPTER) {
            if (r.getEdition() != null) sb.append(r.getEdition()).append(" ed. ");
            if (r.getPublisher() != null) sb.append(r.getPublisher()).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else if (r.getSourceType() == ReferenceSourceType.CONFERENCE_PAPER) {
            if (r.getConferenceTitle() != null) sb.append("in ").append(italic(r.getConferenceTitle())).append(", ");
            if (r.getPageStart() != null) {
                sb.append("pp. ").append(r.getPageStart());
                if (r.getPageEnd() != null) sb.append("–").append(r.getPageEnd());
                sb.append(", ");
            }
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else if (r.getSourceType() == ReferenceSourceType.DISSERTATION) {
            String deg = r.getDegree() != null ? r.getDegree() : "PhD dissertation";
            sb.append(deg).append(", ");
            if (r.getInstitution() != null) sb.append(r.getInstitution()).append(", ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else {
            if (r.getWebsiteName() != null) sb.append("[Online]. ").append(r.getWebsiteName()).append(". ");
            if (r.getUrl() != null) sb.append("Available: ").append(r.getUrl()).append(". ");
            if (r.getAccessDate() != null) sb.append("[Accessed: ").append(r.getAccessDate()).append("].");
        }
        return sb.toString().trim();
    }

    private void appendAuthorsIeee(StringBuilder sb, String json) {
        List<Map<String, String>> contributors = parseContributors(json);
        List<String> authors = contributors.stream()
                .filter(c -> "AUTHOR".equalsIgnoreCase(c.getOrDefault("role", "AUTHOR")))
                .map(c -> {
                    String first = c.getOrDefault("firstName", "");
                    String last = c.getOrDefault("lastName", "");
                    String initials = first.isEmpty() ? "" : first.charAt(0) + ". ";
                    return initials + last;
                })
                .filter(s -> !s.isBlank())
                .toList();
        sb.append(String.join(", ", authors));
        if (!authors.isEmpty()) sb.append(", ");
    }

    // ── Vancouver ──────────────────────────────────────────────────────────────

    private String formatVancouver(ReferenceRequest r) {
        // Author AB, Author CD. Title. J Abbrev. YYYY Mon;Vol(Issue):Pages.
        StringBuilder sb = new StringBuilder();
        appendAuthorsVancouver(sb, r.getContributorsJson());
        sb.append(r.getTitle()).append(". ");
        if (r.getSourceType() == ReferenceSourceType.JOURNAL_ARTICLE && r.getJournalName() != null) {
            sb.append(r.getJournalName()).append(". ");
            if (r.getPubYear() != null) sb.append(r.getPubYear());
            if (r.getPubMonth() != null) sb.append(" ").append(monthAbbrev(r.getPubMonth()));
            sb.append(";");
            if (r.getVolumeStart() != null) sb.append(r.getVolumeStart());
            if (r.getIssue() != null) sb.append("(").append(r.getIssue()).append(")");
            sb.append(":");
            if (r.getPageStart() != null) {
                sb.append(r.getPageStart());
                if (r.getPageEnd() != null) sb.append("-").append(r.getPageEnd());
            }
            sb.append(".");
            if (r.getDoi() != null) sb.append(" doi:").append(r.getDoi()).append(".");
        } else if (r.getSourceType() == ReferenceSourceType.BOOK || r.getSourceType() == ReferenceSourceType.BOOK_CHAPTER) {
            if (r.getPublisher() != null) sb.append(r.getPublisher()).append("; ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else if (r.getSourceType() == ReferenceSourceType.CONFERENCE_PAPER) {
            if (r.getConferenceTitle() != null) sb.append("In: ").append(r.getConferenceTitle()).append("; ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(". ");
            if (r.getPageStart() != null) {
                sb.append("p. ").append(r.getPageStart());
                if (r.getPageEnd() != null) sb.append("-").append(r.getPageEnd());
                sb.append(".");
            }
        } else if (r.getSourceType() == ReferenceSourceType.DISSERTATION) {
            String deg = r.getDegree() != null ? r.getDegree() : "Dissertation";
            sb.append("[").append(deg).append("]. ");
            if (r.getInstitution() != null) sb.append(r.getInstitution()).append("; ");
            if (r.getPubYear() != null) sb.append(r.getPubYear()).append(".");
        } else {
            if (r.getWebsiteName() != null) sb.append(r.getWebsiteName()).append(". ");
            if (r.getUrl() != null) sb.append("Available from: ").append(r.getUrl()).append(". ");
            if (r.getPubYear() != null) sb.append("[").append(r.getPubYear()).append("].");
        }
        return sb.toString().trim();
    }

    private void appendAuthorsVancouver(StringBuilder sb, String json) {
        List<Map<String, String>> contributors = parseContributors(json);
        List<String> authors = contributors.stream()
                .filter(c -> "AUTHOR".equalsIgnoreCase(c.getOrDefault("role", "AUTHOR")))
                .map(c -> {
                    String last = c.getOrDefault("lastName", "");
                    String first = c.getOrDefault("firstName", "");
                    StringBuilder initials = new StringBuilder();
                    for (String part : first.split(" ")) {
                        if (!part.isEmpty()) initials.append(Character.toUpperCase(part.charAt(0)));
                    }
                    return last + " " + initials;
                })
                .filter(s -> !s.isBlank())
                .toList();
        sb.append(String.join(", ", authors));
        if (!authors.isEmpty()) sb.append(". ");
    }

    private String monthAbbrev(int m) {
        return switch (m) {
            case 1 -> "Jan"; case 2 -> "Feb"; case 3 -> "Mar";
            case 4 -> "Apr"; case 5 -> "May"; case 6 -> "Jun";
            case 7 -> "Jul"; case 8 -> "Aug"; case 9 -> "Sep";
            case 10 -> "Oct"; case 11 -> "Nov"; case 12 -> "Dec";
            default -> "";
        };
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private List<Map<String, String>> parseContributors(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse contributorsJson: {}", e.getMessage());
            return List.of();
        }
    }

    private void appendAuthorsApa(StringBuilder sb, String json) {
        List<Map<String, String>> contributors = parseContributors(json);
        List<String> authors = contributors.stream()
                .filter(c -> "AUTHOR".equalsIgnoreCase(c.getOrDefault("role", "AUTHOR")))
                .map(c -> {
                    String last = c.getOrDefault("lastName", "");
                    String first = c.getOrDefault("firstName", "");
                    String initials = first.isEmpty() ? "" : ", " + first.charAt(0) + ".";
                    return last + initials;
                })
                .filter(s -> !s.isBlank())
                .toList();
        if (authors.isEmpty()) return;
        if (authors.size() == 1) {
            sb.append(authors.get(0)).append(". ");
        } else if (authors.size() == 2) {
            sb.append(authors.get(0)).append(", & ").append(authors.get(1)).append(". ");
        } else {
            sb.append(String.join(", ", authors.subList(0, authors.size() - 1)))
              .append(", & ").append(authors.get(authors.size() - 1)).append(". ");
        }
    }

    private void appendAuthorsMla(StringBuilder sb, String json) {
        List<Map<String, String>> contributors = parseContributors(json);
        if (contributors.isEmpty()) return;
        Map<String, String> first = contributors.get(0);
        String last = first.getOrDefault("lastName", "");
        String firstName = first.getOrDefault("firstName", "");
        sb.append(last).append(", ").append(firstName);
        for (int i = 1; i < contributors.size(); i++) {
            Map<String, String> c = contributors.get(i);
            sb.append(", ").append(c.getOrDefault("firstName", ""))
              .append(" ").append(c.getOrDefault("lastName", ""));
        }
        sb.append(". ");
    }

    private void appendAuthorsHarvard(StringBuilder sb, String json) {
        List<Map<String, String>> contributors = parseContributors(json);
        List<String> names = contributors.stream()
                .map(c -> {
                    String last = c.getOrDefault("lastName", "");
                    String f = c.getOrDefault("firstName", "");
                    String initial = f.isEmpty() ? "" : f.charAt(0) + ".";
                    return last + (initial.isEmpty() ? "" : ", " + initial);
                })
                .filter(s -> !s.isBlank())
                .toList();
        sb.append(String.join(" and ", names));
    }

    private void appendAuthorsChicago(StringBuilder sb, String json) {
        appendAuthorsMla(sb, json);
    }

    private void appendYear(StringBuilder sb, Integer year) {
        if (year != null) sb.append("(").append(year).append("). ");
        else sb.append("(n.d.). ");
    }

    private void appendDate(StringBuilder sb, Integer year, Integer month, Integer day) {
        sb.append("(");
        if (year != null) sb.append(year);
        if (month != null) sb.append(", ").append(monthName(month));
        if (day != null) sb.append(" ").append(day);
        sb.append("). ");
    }

    private void appendPages(StringBuilder sb, String start, String end) {
        if (start != null) {
            sb.append(", pp. ").append(start);
            if (end != null) sb.append("–").append(end);
        }
    }

    private void appendPagesMla(StringBuilder sb, String start, String end) {
        if (start != null) {
            sb.append("pp. ").append(start);
            if (end != null) sb.append("–").append(end);
            sb.append(". ");
        }
    }

    private void appendDoi(StringBuilder sb, String doi) {
        if (doi != null && !doi.isBlank()) sb.append(". https://doi.org/").append(doi);
    }

    private String italic(String text) {
        if (text == null) return "";
        return "<em>" + text + "</em>";
    }

    private String monthName(int m) {
        return switch (m) {
            case 1 -> "January"; case 2 -> "February"; case 3 -> "March";
            case 4 -> "April"; case 5 -> "May"; case 6 -> "June";
            case 7 -> "July"; case 8 -> "August"; case 9 -> "September";
            case 10 -> "October"; case 11 -> "November"; case 12 -> "December";
            default -> "";
        };
    }
}
