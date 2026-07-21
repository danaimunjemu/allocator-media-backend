package com.allocator.marketdataservice.service;

import com.allocator.authservice.service.IntegrationSettingService;
import com.allocator.marketdataservice.dto.MarketTickerItemDto;
import com.allocator.marketdataservice.model.MarketTickerItem;
import com.allocator.marketdataservice.repository.MarketTickerItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live African market ticker data via Mansa API (https://mansaapi.com — free
 * tier: 100 req/day). Rather than calling Mansa on every page load (which
 * would exhaust the daily quota after ~100 visitors), a scheduled job pulls
 * data every 6 hours (1 request for ALL exchange indices + 2 for individual
 * stock quotes = ~12 requests/day total) and persists it to
 * market_ticker_items. GET /api/v1/market-ticker (MarketDataController)
 * always reads from that table — it never calls Mansa directly.
 * <p>
 * Every exchange currently reporting status=live with a non-null
 * index_change_pct is included (not a hand-picked handful) — that single
 * /markets/exchanges call typically returns ~15 usable indices, which is
 * what fills out the ticker; no fabricated/placeholder entries are ever
 * mixed into the real set (see FALLBACK_ITEMS below — used only when the
 * table has never been populated at all).
 * <p>
 * The API key comes from Settings > Integrations (encrypted in
 * platform_settings, see IntegrationSettingService), falling back to the
 * mansa.api-key/MANSA_API_KEY env var if nothing is configured there — same
 * "DB takes priority over env" pattern as StripeConfig.
 * <p>
 * Verified directly against the real API (2026-07-15): responses are wrapped
 * in {"success", "data", "meta"}; exchange codes are country names (KENYA,
 * SOUTHAFRICA, NIGERIA, EGYPT — not NSE/JSE/NGX/EGX); per-exchange index
 * change is `index_change_pct` on GET /markets/exchanges; per-stock change
 * is `change_pct` on GET /markets/exchanges/{code}/stocks/{ticker}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MansaMarketDataService {

    private static final String MANSA_BASE_URL = "https://mansaapi.com/api/v1";
    private static final String EXCHANGE_KEY_PREFIX = "exchange:";

    // Matches a short (2-6 letter) acronym in a trailing parenthetical, e.g.
    // "Nairobi Securities Exchange (NSE)" -> "NSE". Deliberately rejects long
    // descriptive parens like BRVM's "BRVM (Bourse Régionale des Valeurs
    // Mobilières)" — there the real acronym is the leading word, not inside
    // the parens, so that one falls back to just the country name instead.
    private static final Pattern ACRONYM_PATTERN = Pattern.compile("\\(([A-Za-z]{2,6})\\)\\s*$");

    private record StockSpec(String itemKey, String exchangeCode, String ticker, String displayName, int sortOrder) {
    }

    private static final List<StockSpec> STOCK_SPECS = List.of(
            new StockSpec("stock:KENYA:SCOM", "KENYA", "SCOM", "Safaricom (SCOM)", 1000),
            new StockSpec("stock:SOUTHAFRICA:MTN", "SOUTHAFRICA", "MTN", "MTN Group (MTN)", 1001)
    );

    private static final List<MarketTickerItemDto> FALLBACK_ITEMS = List.of(
            new MarketTickerItemDto("NSE (Kenya)", 0.42),
            new MarketTickerItemDto("JSE (South Africa)", -0.65),
            new MarketTickerItemDto("NGX (Nigeria)", 1.12),
            new MarketTickerItemDto("EGX (Egypt)", -0.28),
            new MarketTickerItemDto("Safaricom (SCOM)", 0.95),
            new MarketTickerItemDto("MTN Group (MTN)", -1.04)
    );

    private final IntegrationSettingService integrationSettingService;
    private final MarketTickerItemRepository repository;
    private final RestClient restClient = RestClient.create();

    @Value("${mansa.api-key:}")
    private String envApiKey;

    /** Read path — always from the DB, never calls Mansa. Placeholder only if the table has never been populated. */
    public List<MarketTickerItemDto> getTickerItems() {
        List<MarketTickerItem> rows = repository.findAllByOrderBySortOrderAsc();
        if (rows.isEmpty()) {
            return FALLBACK_ITEMS;
        }
        return rows.stream()
                .map(row -> new MarketTickerItemDto(row.getDisplayName(), row.getChangePercent()))
                .toList();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refreshMarketData();
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void refreshMarketData() {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Mansa API key not configured (Settings > Integrations or MANSA_API_KEY) — skipping market data refresh.");
            return;
        }

        refreshExchangeIndices(apiKey);
        refreshStockQuotes(apiKey);
        log.info("Mansa market data refresh complete.");
    }

    private void refreshExchangeIndices(String apiKey) {
        JsonNode exchanges = get("/markets/exchanges", apiKey);
        if (exchanges == null) {
            log.warn("Mansa /markets/exchanges request failed — leaving existing index rows in place.");
            return;
        }

        JsonNode exchangesArray = exchanges.path("data");
        if (!exchangesArray.isArray()) {
            log.warn("Mansa /markets/exchanges returned no data array — leaving existing index rows in place.");
            return;
        }

        Set<String> seenKeys = new HashSet<>();
        int sortOrder = 0;
        for (JsonNode exchange : exchangesArray) {
            String code = textOrNull(exchange, "code");
            String status = textOrNull(exchange, "status");
            JsonNode changeNode = exchange.get("index_change_pct");
            if (code == null || !"live".equalsIgnoreCase(status) || changeNode == null || !changeNode.isNumber()) {
                continue;
            }

            String itemKey = EXCHANGE_KEY_PREFIX + code;
            String displayName = shortDisplayName(exchange);
            upsert(itemKey, displayName, changeNode.asDouble(), sortOrder++, textOrNull(exchange, "last_updated"));
            seenKeys.add(itemKey);
        }

        // Drop any exchange that no longer qualifies (went offline, lost its
        // index value, etc.) rather than showing a stale number forever.
        List<MarketTickerItem> existingExchangeRows = repository.findByItemKeyStartingWith(EXCHANGE_KEY_PREFIX);
        List<MarketTickerItem> stale = existingExchangeRows.stream()
                .filter(row -> !seenKeys.contains(row.getItemKey()))
                .toList();
        if (!stale.isEmpty()) {
            repository.deleteAll(stale);
            log.info("Removed {} stale exchange ticker row(s) no longer live: {}", stale.size(),
                    stale.stream().map(MarketTickerItem::getItemKey).toList());
        }
    }

    private void refreshStockQuotes(String apiKey) {
        for (StockSpec spec : STOCK_SPECS) {
            JsonNode quote = get("/markets/exchanges/" + spec.exchangeCode() + "/stocks/" + spec.ticker(), apiKey);
            if (quote == null) {
                log.warn("Mansa stock quote request failed for {}/{} — leaving existing row in place.",
                        spec.exchangeCode(), spec.ticker());
                continue;
            }
            JsonNode data = quote.path("data");
            JsonNode changeNode = data.get("change_pct");
            if (changeNode != null && changeNode.isNumber()) {
                upsert(spec.itemKey(), spec.displayName(), changeNode.asDouble(), spec.sortOrder(),
                        textOrNull(data, "last_updated"));
            }
        }
    }

    /** e.g. "Nairobi Securities Exchange (NSE)" + country "Kenya" -> "NSE (Kenya)"; falls back to just the country name. */
    private String shortDisplayName(JsonNode exchange) {
        String name = textOrNull(exchange, "name");
        String country = textOrNull(exchange, "country");
        if (name != null) {
            Matcher matcher = ACRONYM_PATTERN.matcher(name);
            if (matcher.find()) {
                String acronym = matcher.group(1).toUpperCase();
                return country != null ? acronym + " (" + country + ")" : acronym;
            }
        }
        return country != null ? country : name;
    }

    private String resolveApiKey() {
        String dbKey = integrationSettingService.getMansaApiKey();
        if (dbKey != null && !dbKey.isBlank()) return dbKey;
        return envApiKey;
    }

    private JsonNode get(String path, String apiKey) {
        try {
            return restClient.get()
                    .uri(MANSA_BASE_URL + path)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("Mansa API request failed for {}: {}", path, e.getMessage());
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    // Not @Transactional: called via internal self-invocation (bypasses the Spring proxy
    // anyway), and repository.save() already demarcates its own transaction per call.
    private void upsert(String itemKey, String displayName, double changePercent, int sortOrder, String marketLastUpdatedIso) {
        MarketTickerItem row = repository.findByItemKey(itemKey)
                .orElse(MarketTickerItem.builder().itemKey(itemKey).build());
        row.setDisplayName(displayName);
        row.setChangePercent(changePercent);
        row.setSortOrder(sortOrder);
        row.setMarketLastUpdatedAt(parseInstant(marketLastUpdatedIso));
        repository.save(row);
    }

    private LocalDateTime parseInstant(String iso) {
        if (iso == null) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
