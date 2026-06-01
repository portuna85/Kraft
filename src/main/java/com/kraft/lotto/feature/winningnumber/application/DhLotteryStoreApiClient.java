package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public class DhLotteryStoreApiClient implements WinningStoreApiClient {

    private static final Logger log = LoggerFactory.getLogger(DhLotteryStoreApiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public DhLotteryStoreApiClient(RestClient restClient, ObjectMapper objectMapper, String baseUrl) {
        this.restClient   = restClient;
        this.objectMapper = objectMapper;
        this.baseUrl      = baseUrl;
    }

    @Override
    public List<WinningStore> fetchStores(int round, int grade) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("method", "searchStoreOfDraw")
                .queryParam("drwNo", round)
                .queryParam("winGrade", grade)
                .build()
                .toUri();
        try {
            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                log.warn("winning store API returned blank body: round={}, grade={}", round, grade);
                return List.of();
            }
            return parse(round, grade, body);
        } catch (Exception ex) {
            log.warn("winning store API call failed: round={}, grade={}, reason={}", round, grade, ex.getMessage());
            return List.of();
        }
    }

    private List<WinningStore> parse(int round, int grade, String body) {
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
                stores.add(new WinningStore(round, grade, name.trim(),
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
