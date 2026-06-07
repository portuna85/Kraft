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

class RelayStoreApiClient implements WinningStoreApiClient {

    private static final Logger log = LoggerFactory.getLogger(RelayStoreApiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String relayUrl;

    RelayStoreApiClient(RestClient restClient, ObjectMapper objectMapper, String relayUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.relayUrl = relayUrl;
    }

    @Override
    public List<WinningStore> fetchStores(int round, int grade) {
        var uri = UriComponentsBuilder.fromUriString(relayUrl)
                .queryParam("round", round)
                .queryParam("grade", grade)
                .build().toUri();
        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                log.warn("relay returned empty body: round={}, grade={}", round, grade);
                return List.of();
            }
            return parse(round, grade, body);
        } catch (Exception e) {
            log.warn("relay call failed: round={}, grade={}, reason={}", round, grade, e.getMessage());
            return List.of();
        }
    }

    private List<WinningStore> parse(int round, int grade, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode arr = root.path("stores");
            if (arr.isMissingNode() || !arr.isArray()) {
                log.warn("relay response unexpected format: round={}, grade={}", round, grade);
                return List.of();
            }
            List<WinningStore> stores = new ArrayList<>();
            for (JsonNode node : arr) {
                String name = node.path("name").asText(null);
                String address = node.path("address").asText("");
                int winCount = node.path("winCount").asInt(1);
                if (name == null || name.isBlank()) {
                    continue;
                }
                stores.add(new WinningStore(round, grade, name.trim(), address.trim(), winCount));
            }
            return List.copyOf(stores);
        } catch (Exception e) {
            log.warn("relay parse failed: round={}, grade={}, reason={}", round, grade, e.getMessage());
            return List.of();
        }
    }
}
