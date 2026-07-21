package com.allocator.contentservice.extractor;

import com.allocator.contentservice.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArxivExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;

    private static final String ARXIV_API = "https://export.arxiv.org/api/query?id_list=";

    @Override public String getName() { return "arXiv"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.ARXIV; }
    @Override public int getPriority() { return 1; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String url = ARXIV_API + identifier.getValue();
            String xml = restTemplate.getForObject(url, String.class);
            if (xml == null || !xml.contains("<entry>")) {
                return ExtractionResult.failed(getName(), "No arXiv entry found");
            }

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(identifier.getNormalizedInput())
                    .resourceType(ResourceType.RESEARCH_PAPER)
                    .arxivId(identifier.getValue())
                    .url("https://arxiv.org/abs/" + identifier.getValue())
                    .institution("arXiv");

            src.title(extractXmlText(xml, "title"));
            src.abstractText(extractXmlText(xml, "summary"));
            src.publisher("arXiv");
            src.containerTitle("arXiv");

            // Authors
            List<CitationAuthor> authors = new ArrayList<>();
            Pattern authorPattern = Pattern.compile("<author>\\s*<name>([^<]+)</name>", Pattern.DOTALL);
            Matcher authorMatcher = authorPattern.matcher(xml);
            while (authorMatcher.find()) {
                String fullName = authorMatcher.group(1).trim();
                String[] parts = fullName.split(" ");
                String lastName = parts[parts.length - 1];
                String firstName = parts.length > 1 ? fullName.substring(0, fullName.length() - lastName.length()).trim() : "";
                authors.add(CitationAuthor.builder().firstName(firstName).lastName(lastName).role("AUTHOR").build());
            }
            src.authors(authors);

            // Published date
            String published = extractXmlText(xml, "published");
            if (published != null && published.length() >= 7) {
                try { src.pubYear(Integer.parseInt(published.substring(0, 4))); } catch (Exception ignored) {}
                try { src.pubMonth(Integer.parseInt(published.substring(5, 7))); } catch (Exception ignored) {}
            }

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(92)
                    .extractorName(getName())
                    .requiresManualCompletion(false)
                    .build();

        } catch (Exception e) {
            log.warn("arXiv extraction failed for {}: {}", identifier.getValue(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }

    private String extractXmlText(String xml, String tag) {
        Pattern p = Pattern.compile("<" + tag + "[^>]*>([^<]*)</" + tag + ">", Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim().replaceAll("\\s+", " ") : null;
    }
}
