package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public class DhLotteryStoreApiClient implements WinningStoreApiClient {

    private static final Logger log = LoggerFactory.getLogger(DhLotteryStoreApiClient.class);
    private static final URI DH_URI = URI.create("https://www.dhlottery.co.kr");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String sessionSeedUrl;
    private final CookieManager cookieManager;
    private final DhLotteryTracerClient tracerClient;
    private final String userAgent;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "RestClient, ObjectMapper, CookieManager are shared application-scoped beans")
    public DhLotteryStoreApiClient(RestClient restClient, ObjectMapper objectMapper,
                                   String baseUrl, String sessionSeedUrl,
                                   CookieManager cookieManager,
                                   DhLotteryTracerClient tracerClient,
                                   String userAgent) {
        this.restClient     = restClient;
        this.objectMapper   = objectMapper;
        this.baseUrl        = baseUrl;
        this.sessionSeedUrl = sessionSeedUrl;
        this.cookieManager  = cookieManager;
        this.tracerClient   = tracerClient;
        this.userAgent      = userAgent != null ? userAgent : "";
    }

    public DhLotteryStoreApiClient(RestClient restClient, ObjectMapper objectMapper, String baseUrl) {
        this(restClient, objectMapper, baseUrl, null, null, null, null);
    }

    @Override
    public List<WinningStore> fetchStores(int round, int grade) {
        establishSession(round);
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("method", "searchStoreOfDraw")
                .queryParam("drwNo", round)
                .queryParam("winGrade", grade)
                .build()
                .toUri();
        try {
            String body = restClient.get()
                    .uri(uri)
                    .header("Referer", sessionSeedUrl != null
                            ? sessionSeedUrl + "&drwNoSelect=" + round
                            : baseUrl)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                log.warn("winning store API returned blank body: round={}, grade={}", round, grade);
                return List.of();
            }
            if (body.trim().startsWith("<")) {
                log.warn("winning store API returned HTML (session may have failed): round={}, grade={}", round, grade);
                return List.of();
            }
            return parse(round, grade, body);
        } catch (Exception ex) {
            log.warn("winning store API call failed: round={}, grade={}, reason={}", round, grade, ex.getMessage());
            return List.of();
        }
    }

    private void establishSession(int round) {
        if (sessionSeedUrl == null || sessionSeedUrl.isBlank()) {
            return;
        }
        try {
            ensureWcCookie();
            String pageUrl = sessionSeedUrl + "&drwNoSelect=" + round;
            restClient.get().uri(URI.create(pageUrl)).retrieve().toBodilessEntity();
            log.debug("store session established: round={}", round);
            if (tracerClient != null) {
                tracerClient.performHandshake(pageUrl, getWcCookie(), userAgent);
            }
        } catch (Exception ex) {
            log.warn("store session establishment failed: round={}, reason={}", round, ex.getMessage());
        }
    }

    void ensureWcCookie() {
        if (cookieManager == null) {
            return;
        }
        try {
            boolean exists = cookieManager.getCookieStore().get(DH_URI)
                    .stream().anyMatch(c -> "wcCookie".equals(c.getName()));
            if (!exists) {
                HttpCookie cookie = new HttpCookie("wcCookie", DhLotteryTracerClient.generateWcCookie());
                cookie.setDomain(".dhlottery.co.kr");
                cookie.setPath("/");
                cookie.setMaxAge(365L * 24 * 60 * 60);
                cookieManager.getCookieStore().add(DH_URI, cookie);
                log.debug("wcCookie created: {}", cookie.getValue());
            }
        } catch (Exception e) {
            log.debug("wcCookie setup failed: {}", e.getMessage());
        }
    }

    String getWcCookie() {
        if (cookieManager == null) {
            return "";
        }
        try {
            return cookieManager.getCookieStore().get(DH_URI).stream()
                    .filter(c -> "wcCookie".equals(c.getName()))
                    .map(HttpCookie::getValue)
                    .findFirst().orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    List<WinningStore> parse(int round, int grade, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode arr = root.path("arrWinInfo");
            if (arr.isMissingNode() || !arr.isArray()) {
                log.warn("winning store API unexpected format: round={}, grade={}, preview={}",
                        round, grade, preview(body));
                return List.of();
            }
            List<WinningStore> stores = new ArrayList<>();
            for (JsonNode node : arr) {
                String name    = node.path("BPLC_NM").asText(null);
                String address = node.path("BPLC_ADRS").asText(null);
                int winCount   = parseWinCount(node.path("WIN_CNT").asText("1"));
                if (name == null || name.isBlank()) {
                    continue;
                }
                stores.add(WinningStore.of(round, grade, name.trim(),
                        address == null ? "" : address.trim(), winCount));
            }
            return List.copyOf(stores);
        } catch (Exception ex) {
            log.warn("winning store API parse failed: round={}, grade={}, reason={}", round, grade, ex.getMessage());
            return List.of();
        }
    }

    private static int parseWinCount(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String preview(String body) {
        return body.substring(0, Math.min(80, body.length())).replaceAll("\\s+", " ");
    }
}
