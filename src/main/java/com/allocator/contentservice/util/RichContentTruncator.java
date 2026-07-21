package com.allocator.contentservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

// Truncates a TipTap/Editor.js-style rich-text JSON document down to roughly
// the first `fraction` of its total text length, cutting only at top-level
// node boundaries (never splitting inside a node, which risks malformed
// TipTap JSON). Used to enforce the paywall preview server-side — the
// truncation happens to the JSON payload itself, not hidden client-side.
@Slf4j
public final class RichContentTruncator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RichContentTruncator() {
    }

    public static String truncateToFraction(String tipTapJson, double fraction) {
        if (tipTapJson == null || tipTapJson.isBlank()) {
            return tipTapJson;
        }

        try {
            JsonNode root = MAPPER.readTree(tipTapJson);
            JsonNode contentNode = root.get("content");
            if (contentNode == null || !contentNode.isArray() || contentNode.isEmpty()) {
                // Not a doc with top-level blocks (e.g. plain body text stored
                // as a bare string that happens to parse as JSON) — fall back
                // to a plain character-based cut.
                return truncatePlainText(tipTapJson, fraction);
            }

            ArrayNode topLevelNodes = (ArrayNode) contentNode;
            int totalChars = 0;
            List<Integer> nodeLengths = new ArrayList<>();
            for (JsonNode node : topLevelNodes) {
                int len = sumTextLength(node);
                nodeLengths.add(len);
                totalChars += len;
            }

            if (totalChars == 0) {
                return tipTapJson;
            }

            int threshold = (int) Math.ceil(totalChars * fraction);
            int cumulative = 0;
            int keepCount = 0;
            for (int len : nodeLengths) {
                keepCount++;
                cumulative += len;
                if (cumulative >= threshold) {
                    break;
                }
            }

            // Always keep at least 1 node; if there's more than one node total,
            // never keep ALL of them — a locked article must visibly cut something.
            keepCount = Math.max(1, keepCount);
            if (topLevelNodes.size() > 1) {
                keepCount = Math.min(keepCount, topLevelNodes.size() - 1);
            }

            ArrayNode kept = MAPPER.createArrayNode();
            for (int i = 0; i < keepCount; i++) {
                kept.add(topLevelNodes.get(i));
            }

            ObjectNode truncatedRoot = root.deepCopy();
            truncatedRoot.set("content", kept);
            return MAPPER.writeValueAsString(truncatedRoot);
        } catch (Exception e) {
            log.warn("Failed to truncate rich content for paywall preview, leaving unchanged", e);
            return tipTapJson;
        }
    }

    private static int sumTextLength(JsonNode node) {
        int sum = 0;
        if (node.has("text") && node.get("text").isTextual()) {
            sum += node.get("text").asText().length();
        }
        JsonNode children = node.get("content");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                sum += sumTextLength(child);
            }
        }
        return sum;
    }

    private static String truncatePlainText(String text, double fraction) {
        int cut = (int) Math.ceil(text.length() * fraction);
        cut = Math.max(1, Math.min(cut, text.length()));
        return text.substring(0, cut);
    }
}
