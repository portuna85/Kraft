package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 공공데이터포털 당첨 판매점 API 클라이언트.
 * WinningStoreApiClient의 fallback으로 사용된다.
 * API: https://apis.data.go.kr/B551014/OnlineRtl/getStoreInfo
 */
class PublicDataStoreApiClient implements WinningStoreApiClient {

    private static final Logger log = LoggerFactory.getLogger(PublicDataStoreApiClient.class);
    private static final String DEFAULT_PATH = "/B551014/OnlineRtl/getStoreInfo";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    PublicDataStoreApiClient(RestClient restClient, ObjectMapper objectMapper,
                              String baseUrl, String apiKey) {
        this.restClient   = restClient;
        this.objectMapper = objectMapper;
        this.baseUrl      = baseUrl;
        this.apiKey       = apiKey;
    }

    @Override
    public List<WinningStore> fetchStores(int round, int grade) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl + DEFAULT_PATH)
                .queryParam("serviceKey", apiKey)
                .queryParam("drwNo", round)
                .queryParam("winGrade", grade)
                .queryParam("_type", "json")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .build(true)
                .toUri();
        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                log.warn("public-data store API returned blank body: round={}, grade={}", round, grade);
                return List.of();
            }
            return parse(round, grade, body);
        } catch (Exception ex) {
            log.warn("public-data store API failed: round={}, grade={}, reason={}", round, grade, ex.getMessage());
            return List.of();
        }
    }

    List<WinningStore> parse(int round, int grade, String body) {
        try {
            JsonNode root    = objectMapper.readTree(body);
            JsonNode header  = root.path("response").path("header");
            String resultCode = header.path("resultCode").asText("");
            if (!"00".equals(resultCode)) {
                log.warn("public-data store API non-success resultCode={}: round={}, grade={}",
                        resultCode, round, grade);
                return List.of();
            }
            JsonNode itemNode = root.path("response").path("body").path("items").path("item");
            if (itemNode.isMissingNode() || itemNode.isNull()) {
                return List.of();
            }

            List<WinningStore> stores = new ArrayList<>();
            if (itemNode.isArray()) {
                for (JsonNode node : itemNode) {
                    WinningStore store = toStore(node, round, grade);
                    if (store != null) {
                        stores.add(store);
                    }
                }
            } else {
                WinningStore store = toStore(itemNode, round, grade);
                if (store != null) {
                    stores.add(store);
                }
            }
            return List.copyOf(stores);
        } catch (Exception ex) {
            log.warn("public-data store API parse failed: round={}, grade={}, reason={}", round, grade, ex.getMessage());
            return List.of();
        }
    }

    private static WinningStore toStore(JsonNode node, int round, int grade) {
        String name = node.path("bplcNm").asText(null);
        if (name == null || name.isBlank()) {
            return null;
        }
        String address  = node.path("bplcAdrs").asText("");
        int    winCount = node.path("winCnt").asInt(1);
        return WinningStore.withSource(round, grade, name.trim(), address.trim(), winCount,
                WinningStoreSources.PUBLIC_DATA);
    }
}
