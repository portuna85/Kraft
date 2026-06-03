package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("smok API 클라이언트 테스트")
class SmokLottoApiClientTest {

    private static final String BASE_URL = "https://smok95.github.io/lotto/results";

    private final SmokLottoApiClient client =
            new SmokLottoApiClient(null, new ObjectMapper(), BASE_URL, Clock.systemDefaultZone());

    @Nested
    @DisplayName("응답 파싱")
    class Parse {

        @Test
        @DisplayName("유효한 JSON을 도메인 모델로 변환한다")
        void parseConvertsValidJsonToDomain() {
            Optional<WinningNumber> result = client.parse(100, successBody(100));

            assertThat(result).isPresent();
            WinningNumber wn = result.get();
            assertThat(wn.round()).isEqualTo(100);
            assertThat(wn.drawDate()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(wn.combination().numbers()).containsExactly(1, 2, 3, 4, 5, 6);
            assertThat(wn.bonusNumber()).isEqualTo(7);
        }

        @Test
        @DisplayName("divisions[0]에서 1등 정보를 파싱한다")
        void parseExtractsFirstPrizeFromDivisions() {
            Optional<WinningNumber> result = client.parse(100, successBody(100));

            assertThat(result).isPresent();
            WinningNumber wn = result.get();
            assertThat(wn.firstPrize()).isEqualTo(1_500_000_000L);
            assertThat(wn.firstWinners()).isEqualTo(3);
        }

        @Test
        @DisplayName("draw_no가 요청 회차와 다르면 예외가 발생한다")
        void parseThrowsOnRoundMismatch() {
            assertThatThrownBy(() -> client.parse(100, successBody(101)))
                    .isInstanceOf(LottoApiClientException.class)
                    .hasMessageContaining("mismatch");
        }

        @Test
        @DisplayName("잘못된 JSON 형식이면 예외가 발생한다")
        void parseThrowsOnInvalidJson() {
            assertThatThrownBy(() -> client.parse(100, "not-json"))
                    .isInstanceOf(LottoApiClientException.class);
        }
    }

    @Nested
    @DisplayName("응답 계약 — 필수 필드 부재 시 파싱 실패")
    class Contract {

        @ParameterizedTest(name = "필드 누락: {0}")
        @DisplayName("필수 필드가 없으면 예외가 발생한다")
        @ValueSource(strings = {
                "\"draw_no\": 100,",
                "\"numbers\": [1, 2, 3, 4, 5, 6],",
                "\"bonus_no\": 7,",
                "\"date\": \"2020-01-01T09:00:00+09:00\",",
                "\"divisions\": ["
        })
        void parseThrowsWhenFieldIsAbsent(String fieldFragment) {
            String body = successBody(100).replace(fieldFragment, "");
            assertThatThrownBy(() -> client.parse(100, body))
                    .isInstanceOf(LottoApiClientException.class);
        }

        @Test
        @DisplayName("successBody 픽스처가 완전한 성공 응답으로 파싱된다")
        void fixtureIsValidAndComplete() {
            Optional<WinningNumber> result = client.parse(100, successBody(100));

            assertThat(result).isPresent();
            WinningNumber wn = result.get();
            assertThat(wn.round()).isEqualTo(100);
            assertThat(wn.drawDate()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(wn.combination().numbers()).containsExactly(1, 2, 3, 4, 5, 6);
            assertThat(wn.bonusNumber()).isEqualTo(7);
            assertThat(wn.firstPrize()).isEqualTo(1_500_000_000L);
            assertThat(wn.firstWinners()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("HTTP 응답 처리")
    class Fetch {

        @Test
        @DisplayName("404 응답은 Optional.empty를 반환한다")
        void fetchReturnsEmptyOn404() {
            ScriptedSmokLottoApiClient scriptedClient = new ScriptedSmokLottoApiClient(
                    0,
                    new SmokLottoApiClient.RawResult(404, "")
            );

            Optional<WinningNumber> result = scriptedClient.fetch(999);

            assertThat(result).isEmpty();
            assertThat(scriptedClient.fetchCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("미추첨 404 응답 수신 시 half-open 상태가 closed로 전환된다")
        void halfOpenClosesWhenNotDrawn404IsReturned() {
            AtomicLong now = new AtomicLong(0L);
            ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 1, 1000, 1, now::get);
            breaker.tryAcquirePermission();
            breaker.recordFailure();
            assertThat(breaker.stateName()).isEqualTo("open");
            now.addAndGet(TimeUnit.MILLISECONDS.toNanos(1000));

            ScriptedSmokLottoApiClient scriptedClient = new ScriptedSmokLottoApiClient(
                    0,
                    breaker,
                    new SmokLottoApiClient.RawResult(404, "")
            );

            assertThat(scriptedClient.fetch(999)).isEmpty();
            assertThat(breaker.stateName()).isEqualTo("closed");
        }

        @Test
        @DisplayName("네트워크 오류 시 재시도한다")
        void fetchRetriesOnNetworkFailure() {
            ScriptedSmokLottoApiClient scriptedClient = new ScriptedSmokLottoApiClient(
                    2,
                    new LottoApiClientException("network"),
                    new SmokLottoApiClient.RawResult(200, successBody(100))
            );

            Optional<WinningNumber> result = scriptedClient.fetch(100);

            assertThat(result).isPresent();
            assertThat(scriptedClient.fetchCalls()).isEqualTo(2);
        }

        @Test
        @DisplayName("재시도 횟수 초과 시 예외가 발생한다")
        void fetchThrowsWhenRetriesExhausted() {
            ScriptedSmokLottoApiClient scriptedClient = new ScriptedSmokLottoApiClient(
                    2,
                    new LottoApiClientException("network-1"),
                    new LottoApiClientException("network-2"),
                    new LottoApiClientException("network-3")
            );

            assertThatThrownBy(() -> scriptedClient.fetch(100))
                    .isInstanceOf(LottoApiClientException.class);
            assertThat(scriptedClient.fetchCalls()).isEqualTo(3);
        }

        @Test
        @DisplayName("성공 시 kraft.api.smok.call.total과 success 메트릭을 기록한다")
        void fetchRecordsMetricsOnSuccess() {
            SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
            ScriptedSmokLottoApiClient scriptedClient = new ScriptedSmokLottoApiClient(
                    0,
                    meterRegistry,
                    new SmokLottoApiClient.RawResult(200, successBody(100))
            );

            scriptedClient.fetch(100);

            assertThat(meterRegistry.get("kraft.api.smok.call.total").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.get("kraft.api.smok.call.success").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.get("kraft.api.smok.latency").timer().count()).isEqualTo(1L);
        }

        @Test
        @DisplayName("재시도 중 요청 마감 시간을 초과하면 재시도를 중단한다")
        void fetchStopsRetryingWhenDeadlineExpiresDuringBackoff() {
            AtomicLong now = new AtomicLong(0L);
            List<Long> sleeps = new ArrayList<>();
            ApiRetrySupport retrySupport = new ApiRetrySupport(
                    100,
                    100,
                    now::get,
                    () -> 0.5d,
                    delayMs -> {
                        sleeps.add(delayMs);
                        now.addAndGet(TimeUnit.MILLISECONDS.toNanos(delayMs));
                    }
            );
            ScriptedSmokLottoApiClient scriptedClient = new ScriptedSmokLottoApiClient(
                    2,
                    retrySupport,
                    new LottoApiClientException("network")
            );

            assertThatThrownBy(() -> scriptedClient.fetch(100))
                    .isInstanceOf(ApiRequestTimeoutException.class);

            assertThat(sleeps).containsExactly(100L);
            assertThat(scriptedClient.fetchCalls()).isEqualTo(1);
        }
    }

    private static final class ScriptedSmokLottoApiClient extends SmokLottoApiClient {

        private final List<Object> scripted;
        private int fetchCalls;

        ScriptedSmokLottoApiClient(int maxRetries, Object... scripted) {
            this(maxRetries, null, new SimpleMeterRegistry(), scripted);
        }

        ScriptedSmokLottoApiClient(int maxRetries, SimpleMeterRegistry meterRegistry, Object... scripted) {
            this(maxRetries, null, meterRegistry, scripted);
        }

        ScriptedSmokLottoApiClient(int maxRetries, ApiRetrySupport retrySupport, Object... scripted) {
            this(maxRetries, retrySupport, new SimpleMeterRegistry(), scripted);
        }

        ScriptedSmokLottoApiClient(int maxRetries, ApiCircuitBreaker circuitBreaker, Object... scripted) {
            this(maxRetries, null, new SimpleMeterRegistry(), circuitBreaker, scripted);
        }

        private ScriptedSmokLottoApiClient(int maxRetries,
                                           ApiRetrySupport retrySupport,
                                           SimpleMeterRegistry meterRegistry,
                                           Object... scripted) {
            this(maxRetries, retrySupport, meterRegistry, ApiCircuitBreaker.disabled(), scripted);
        }

        private ScriptedSmokLottoApiClient(int maxRetries,
                                           ApiRetrySupport retrySupport,
                                           SimpleMeterRegistry meterRegistry,
                                           ApiCircuitBreaker circuitBreaker,
                                           Object... scripted) {
            super(
                    null,
                    new ObjectMapper(),
                    BASE_URL,
                    maxRetries,
                    meterRegistry,
                    Clock.systemDefaultZone(),
                    retrySupport == null ? new ApiRetrySupport(0, 0) : retrySupport,
                    circuitBreaker
            );
            this.scripted = new ArrayList<>(List.of(scripted));
        }

        @Override
        RawResult doFetch(URI uri) {
            fetchCalls++;
            if (scripted.isEmpty()) {
                throw new LottoApiClientException("no scripted response");
            }
            Object next = scripted.remove(0);
            if (next instanceof RuntimeException ex) {
                throw ex;
            }
            return (RawResult) next;
        }

        int fetchCalls() {
            return fetchCalls;
        }
    }

    private static String successBody(int round) {
        return """
                {%n\
                  "draw_no": %d,%n\
                  "numbers": [1, 2, 3, 4, 5, 6],%n\
                  "bonus_no": 7,%n\
                  "date": "2020-01-01T09:00:00+09:00",%n\
                  "total_sales_amount": 2000000000,%n\
                  "divisions": [%n\
                    { "prize": 1500000000, "winners": 3 }%n\
                  ]%n\
                }%n\
                """.formatted(round);
    }
}
