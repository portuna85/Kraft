package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

@DisplayName("RelayStoreApiClient")
class RelayStoreApiClientTest {

    RelayStoreApiClient client;

    @BeforeEach
    void setUp() {
        client = new RelayStoreApiClient(
                mock(RestClient.class), new ObjectMapper(), "http://relay/stores");
    }

    // --- parse ---

    @Test
    @DisplayName("정상 JSON에서 판매점 목록을 파싱한다")
    void parsesValidJson() {
        String json = """
                {"stores":[
                  {"name":"행운복권방","address":"서울 강남구 테헤란로 1","winCount":1},
                  {"name":"미래복권","address":"부산 해운대구 센텀로 2","winCount":2}
                ]}""";

        List<WinningStore> stores = client.parse(1227, 1, json);

        assertThat(stores).hasSize(2);
        assertThat(stores.get(0).name()).isEqualTo("행운복권방");
        assertThat(stores.get(0).address()).isEqualTo("서울 강남구 테헤란로 1");
        assertThat(stores.get(0).winCount()).isEqualTo(1);
        assertThat(stores.get(1).winCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("stores 배열이 비어 있으면 빈 목록을 반환한다")
    void returnsEmptyForEmptyStoresArray() {
        assertThat(client.parse(1227, 1, "{\"stores\":[]}")).isEmpty();
    }

    @Test
    @DisplayName("stores 필드가 없으면 빈 목록을 반환한다")
    void returnsEmptyForMissingStoresField() {
        assertThat(client.parse(1227, 1, "{\"other\":[]}")).isEmpty();
    }

    @Test
    @DisplayName("유효하지 않은 JSON이면 빈 목록을 반환한다")
    void returnsEmptyForInvalidJson() {
        assertThat(client.parse(1227, 1, "not-json")).isEmpty();
    }

    @Test
    @DisplayName("상점명이 빈 항목은 건너뛴다")
    void skipsEntryWithBlankName() {
        String json = """
                {"stores":[
                  {"name":"","address":"주소","winCount":1},
                  {"name":"정상상점","address":"정상주소","winCount":1}
                ]}""";

        List<WinningStore> stores = client.parse(1227, 1, json);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).name()).isEqualTo("정상상점");
    }

    @Test
    @DisplayName("winCount 필드가 없으면 기본값 1로 처리한다")
    void defaultsWinCountWhenMissing() {
        String json = """
                {"stores":[{"name":"상점","address":"주소"}]}""";

        List<WinningStore> stores = client.parse(1227, 1, json);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).winCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("address 필드가 없으면 빈 문자열로 처리한다")
    void defaultsAddressWhenMissing() {
        String json = """
                {"stores":[{"name":"상점"}]}""";

        List<WinningStore> stores = client.parse(1227, 1, json);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).address()).isEmpty();
    }

    // --- fetchStores ---

    @Test
    @DisplayName("fetchStores: 정상 응답에서 판매점 목록을 반환한다")
    void fetchStoresReturnsStoresOnSuccess() {
        RestClient restClient = mock(RestClient.class, Mockito.RETURNS_DEEP_STUBS);
        String json = """
                {"stores":[{"name":"행운복권방","address":"서울 강남구","winCount":1}]}""";
        when(restClient.get().uri(any(java.net.URI.class)).retrieve().body(String.class))
                .thenReturn(json);

        RelayStoreApiClient relayClient = new RelayStoreApiClient(
                restClient, new ObjectMapper(), "http://relay/stores");

        List<WinningStore> stores = relayClient.fetchStores(1227, 1);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).name()).isEqualTo("행운복권방");
    }

    @Test
    @DisplayName("fetchStores: 빈 응답이면 빈 목록을 반환한다")
    void fetchStoresReturnsEmptyForBlankBody() {
        RestClient restClient = mock(RestClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(restClient.get().uri(any(java.net.URI.class)).retrieve().body(String.class))
                .thenReturn("");

        RelayStoreApiClient relayClient = new RelayStoreApiClient(
                restClient, new ObjectMapper(), "http://relay/stores");

        assertThat(relayClient.fetchStores(1227, 1)).isEmpty();
    }

    @Test
    @DisplayName("fetchStores: 예외 발생 시 빈 목록을 반환한다")
    void fetchStoresReturnsEmptyOnException() {
        RestClient restClient = mock(RestClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(restClient.get().uri(any(java.net.URI.class)).retrieve().body(String.class))
                .thenThrow(new RuntimeException("connection refused"));

        RelayStoreApiClient relayClient = new RelayStoreApiClient(
                restClient, new ObjectMapper(), "http://relay/stores");

        assertThat(relayClient.fetchStores(1227, 1)).isEmpty();
    }
}
