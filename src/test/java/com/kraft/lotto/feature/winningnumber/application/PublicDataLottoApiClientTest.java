package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PublicDataLottoApiClient")
class PublicDataLottoApiClientTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2024-06-15T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PublicDataLottoApiClient client;

    @BeforeEach
    void setUp() {
        client = new PublicDataLottoApiClient(
                null, objectMapper,
                "https://apis.data.go.kr", "test-key",
                new SimpleMeterRegistry(), FIXED_CLOCK);
    }

    @Test
    @DisplayName("정상 응답을 파싱해 WinningNumber를 반환한다")
    void parsesNormalResponse() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE" },
                    "body": {
                      "items": {
                        "item": {
                          "drwNo": 1156,
                          "drwNoDate": "2024-06-15",
                          "drwtNo1": 3, "drwtNo2": 11, "drwtNo3": 23,
                          "drwtNo4": 30, "drwtNo5": 39, "drwtNo6": 41,
                          "bnusNo": 7,
                          "firstWinamnt": 2000000000,
                          "firstPrzwnerCo": 3,
                          "totSellamnt": 120000000000,
                          "firstAccumamnt": 6000000000,
                          "secondWinamnt": 70054508,
                          "secondPrzwnerCo": 80
                        }
                      }
                    }
                  }
                }
                """;

        Optional<WinningNumber> result = client.parse(1156, body);

        assertThat(result).isPresent();
        WinningNumber wn = result.get();
        assertThat(wn.round()).isEqualTo(1156);
        assertThat(wn.drawDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(wn.combination().numbers()).containsExactly(3, 11, 23, 30, 39, 41);
        assertThat(wn.bonusNumber()).isEqualTo(7);
        assertThat(wn.firstPrize()).isEqualTo(2_000_000_000L);
        assertThat(wn.firstWinners()).isEqualTo(3);
        assertThat(wn.secondPrize()).isEqualTo(70_054_508L);
        assertThat(wn.secondWinners()).isEqualTo(80);
    }

    @Test
    @DisplayName("resultCode가 03이면 Optional.empty()를 반환한다 (미추첨)")
    void returnsEmptyWhenResultCode03() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "03", "resultMsg": "NO DATA" },
                    "body": {}
                  }
                }
                """;

        Optional<WinningNumber> result = client.parse(9999, body);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("item이 없으면 Optional.empty()를 반환한다")
    void returnsEmptyWhenNoItem() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "00" },
                    "body": { "items": {} }
                  }
                }
                """;

        Optional<WinningNumber> result = client.parse(1156, body);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("회차 번호가 불일치하면 예외를 던진다")
    void throwsOnRoundMismatch() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "00" },
                    "body": {
                      "items": {
                        "item": {
                          "drwNo": 1200,
                          "drwNoDate": "2024-06-15",
                          "drwtNo1": 3, "drwtNo2": 11, "drwtNo3": 23,
                          "drwtNo4": 30, "drwtNo5": 39, "drwtNo6": 41,
                          "bnusNo": 7
                        }
                      }
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> client.parse(1156, body))
                .isInstanceOf(LottoApiClientException.class)
                .hasMessageContaining("round mismatch");
    }

    @Test
    @DisplayName("resultCode가 00이 아닌 경우 예외를 던진다")
    void throwsOnNonSuccessResultCode() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "99", "resultMsg": "SYSTEM ERROR" },
                    "body": {}
                  }
                }
                """;

        assertThatThrownBy(() -> client.parse(1156, body))
                .isInstanceOf(LottoApiClientException.class)
                .hasMessageContaining("resultCode=99");
    }

    @Test
    @DisplayName("item이 배열로 감싸인 경우 첫 번째 항목을 사용한다")
    void parsesFirstItemFromArray() {
        String body = """
                {
                  "response": {
                    "header": { "resultCode": "00" },
                    "body": {
                      "items": {
                        "item": [
                          {
                            "drwNo": 1, "drwNoDate": "2002-12-07",
                            "drwtNo1": 10, "drwtNo2": 23, "drwtNo3": 29,
                            "drwtNo4": 33, "drwtNo5": 37, "drwtNo6": 40,
                            "bnusNo": 16
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        Optional<WinningNumber> result = client.parse(1, body);

        assertThat(result).isPresent();
        assertThat(result.get().round()).isEqualTo(1);
    }
}
