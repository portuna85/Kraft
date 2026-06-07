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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

@DisplayName("PublicDataStoreApiClient")
class PublicDataStoreApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PublicDataStoreApiClient client;

    @BeforeEach
    void setUp() {
        RestClient restClient = mock(RestClient.class);
        client = new PublicDataStoreApiClient(
                restClient, objectMapper,
                "https://apis.data.go.kr", "test-api-key");
    }

    @Test
    @DisplayName("정상 응답(배열)을 파싱해 WinningStore 목록을 반환한다")
    void parsesArrayResponse() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE" },
                    "body": {
                      "items": {
                        "item": [
                          { "bplcNm": "행운복권방", "bplcAdrs": "서울특별시 강남구 테헤란로 1", "winCnt": 1 },
                          { "bplcNm": "미래복권", "bplcAdrs": "부산시 해운대구 센텀로 2", "winCnt": 2 }
                        ]
                      }
                    }
                  }
                }
                """;

        List<WinningStore> stores = client.parse(1230, 1, body);

        assertThat(stores).hasSize(2);
        assertThat(stores.get(0).name()).isEqualTo("행운복권방");
        assertThat(stores.get(0).source()).isEqualTo(WinningStoreSources.PUBLIC_DATA);
        assertThat(stores.get(1).winCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("정상 응답(단건 object)을 파싱해 WinningStore 1개를 반환한다")
    void parsesSingleObjectResponse() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE" },
                    "body": {
                      "items": {
                        "item": { "bplcNm": "하나복권방", "bplcAdrs": "경기도 성남시 1", "winCnt": 1 }
                      }
                    }
                  }
                }
                """;

        List<WinningStore> stores = client.parse(1230, 1, body);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).name()).isEqualTo("하나복권방");
        assertThat(stores.get(0).source()).isEqualTo(WinningStoreSources.PUBLIC_DATA);
    }

    @Test
    @DisplayName("resultCode가 00이 아니면 빈 목록을 반환한다")
    void returnsEmptyOnNonSuccessResultCode() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "99", "resultMsg": "ERROR" },
                    "body": {}
                  }
                }
                """;

        List<WinningStore> stores = client.parse(1230, 1, body);

        assertThat(stores).isEmpty();
    }

    @Test
    @DisplayName("빈 items이면 빈 목록을 반환한다")
    void returnsEmptyWhenItemsMissing() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "00" },
                    "body": { "items": {} }
                  }
                }
                """;

        List<WinningStore> stores = client.parse(1230, 1, body);

        assertThat(stores).isEmpty();
    }

    @Test
    @DisplayName("name이 없는 항목은 건너뛴다")
    void skipsItemsWithBlankName() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "00" },
                    "body": {
                      "items": {
                        "item": [
                          { "bplcNm": "", "bplcAdrs": "주소1", "winCnt": 1 },
                          { "bplcNm": "유효복권방", "bplcAdrs": "주소2", "winCnt": 1 }
                        ]
                      }
                    }
                  }
                }
                """;

        List<WinningStore> stores = client.parse(1230, 1, body);

        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).name()).isEqualTo("유효복권방");
    }

    @Test
    @DisplayName("fetchStores에서 예외 발생 시 빈 목록을 반환한다")
    void returnsEmptyOnException() {
        RestClient failingClient = mock(RestClient.class);
        RequestHeadersUriSpec<?> uriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec<?> headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(failingClient.get()).thenReturn((RequestHeadersUriSpec) uriSpec);
        when(uriSpec.uri(any(java.net.URI.class))).thenReturn((RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("connection refused"));

        PublicDataStoreApiClient failClient = new PublicDataStoreApiClient(
                failingClient, objectMapper, "https://apis.data.go.kr", "key");

        List<WinningStore> result = failClient.fetchStores(1230, 1);

        assertThat(result).isEmpty();
    }
}
