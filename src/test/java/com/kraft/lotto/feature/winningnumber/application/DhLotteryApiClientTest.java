package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

@DisplayName("동행복권 에이피아이 클라이언트 테스트")
class DhLotteryApiClientTest {

    private final DhLotteryApiClient client =
            new DhLotteryApiClient(null, new ObjectMapper(), "http://localhost");

    @Nested
    @DisplayName("응답 파싱")
    class Parse {

        @Test
        @DisplayName("유효한 응답을 도메인 모델로 변환한다")
        void parseConvertsValidResponseToDomain() {
            String body = successBody(1102);

            Optional<WinningNumber> result = client.parse(1102, body);

            assertThat(result).isPresent();
            WinningNumber wn = result.get();
            assertThat(wn.round()).isEqualTo(1102);
            assertThat(wn.drawDate()).isEqualTo(LocalDate.of(2024, 1, 6));
        }

        @Test
        @DisplayName("필수 필드가 누락되면 예외가 발생한다")
        void parseThrowsWhenRequiredFieldIsMissing() {
            String body = """
                    {"returnValue": "success", "drwNoDate": "2024-01-06"}
                    """;
            assertThatThrownBy(() -> client.parse(1102, body)).isInstanceOf(LottoApiClientException.class);
        }

        @Test
        @DisplayName("에이치티엠엘 응답은 예외가 발생한다")
        void parseThrowsOnHtmlResponse() {
            assertThatThrownBy(() -> client.parse(1102, "<html>error</html>"))
                    .isInstanceOf(LottoApiClientException.class);
        }

        @Test
        @DisplayName("빈 응답은 예외가 발생한다")
        void parseThrowsOnBlankResponse() {
            assertThatThrownBy(() -> client.parse(1102, " "))
                    .isInstanceOf(LottoApiClientException.class);
        }

        @Test
        @DisplayName("결과가 실패이면 빈 옵셔널을 반환한다")
        void parseReturnsEmptyOnReturnValueFail() {
            String body = """
                    {"returnValue":"fail","drwNo":1102}
                    """;
            assertThat(client.parse(1102, body)).isEmpty();
        }

        @Test
        @DisplayName("회차 정보가 일치하지 않으면 예외가 발생한다")
        void parseThrowsOnRoundMismatch() {
            String body = successBody(1103);
            assertThatThrownBy(() -> client.parse(1102, body))
                    .isInstanceOf(LottoApiClientException.class);
        }
    }

    @Nested
    @DisplayName("응답 계약 — 필수 필드 부재 시 파싱 실패")
    class Contract {

        @ParameterizedTest(name = "필드 누락: {0}")
        @DisplayName("필수 필드가 없으면 예외가 발생한다")
        @ValueSource(strings = {
                "\"returnValue\": \"success\",",
                "\"drwNoDate\": \"2024-01-06\",",
                "\"drwtNo1\": 6,",
                "\"drwtNo2\": 13,",
                "\"drwtNo3\": 23,",
                "\"drwtNo4\": 24,",
                "\"drwtNo5\": 28,",
                "\"drwtNo6\": 33,",
                "\"bnusNo\": 38,",
                "\"firstWinamnt\": 2596477500,",
                "\"firstPrzwnerCo\": 11,"
        })
        void parseThrowsWhenFieldIsAbsent(String fieldFragment) {
            String body = successBody(1102).replace(fieldFragment, "");
            assertThatThrownBy(() -> client.parse(1102, body))
                    .isInstanceOf(LottoApiClientException.class);
        }

        @Test
        @DisplayName("성공 본문 픽스처가 완전한 성공 응답으로 파싱된다")
        void fixtureIsValidAndComplete() {
            Optional<WinningNumber> result = client.parse(1102, successBody(1102));

            assertThat(result).isPresent();
            WinningNumber wn = result.get();
            assertThat(wn.round()).isEqualTo(1102);
            assertThat(wn.drawDate()).isEqualTo(LocalDate.of(2024, 1, 6));
            assertThat(wn.combination().numbers()).containsExactly(6, 13, 23, 24, 28, 33);
            assertThat(wn.bonusNumber()).isEqualTo(38);
            assertThat(wn.firstPrize()).isEqualTo(2596477500L);
            assertThat(wn.firstWinners()).isEqualTo(11);
        }
    }

    @Nested
    @DisplayName("수집 재시도")
    class FetchRetry {

        @Test
        @DisplayName("네트워크 오류 시 재시도한다")
        void fetchRetriesOnNetworkFailure() {
            ScriptedDhLotteryApiClient scriptedClient = new ScriptedDhLotteryApiClient(
                    2,
                    new LottoApiClientException("network"),
                    new DhLotteryApiClient.ApiRawResponse(200, "application/json", successBody(1102))
            );

            Optional<WinningNumber> result = scriptedClient.fetch(1102);

            assertThat(result).isPresent();
            assertThat(scriptedClient.fetchCalls()).isEqualTo(2);
        }

        @Test
        @DisplayName("재시도 횟수를 초과하면 예외가 발생한다")
        void fetchThrowsWhenRetryExhausted() {
            ScriptedDhLotteryApiClient scriptedClient = new ScriptedDhLotteryApiClient(
                    2,
                    new LottoApiClientException("network-1"),
                    new LottoApiClientException("network-2"),
                    new LottoApiClientException("network-3")
            );

            assertThatThrownBy(() -> scriptedClient.fetch(1102))
                    .isInstanceOf(LottoApiClientException.class)
                    .hasMessageContaining("attempts=3");
            assertThat(scriptedClient.fetchCalls()).isEqualTo(3);
        }

        @Test
        @DisplayName("4백 번대 오류 발생 시 재시도하지 않고 예외가 발생한다")
        void fetchThrowsOnHttpErrorWithoutRetryFor4xx() {
            ScriptedDhLotteryApiClient scriptedClient = new ScriptedDhLotteryApiClient(
                    2,
                    new DhLotteryApiClient.ApiRawResponse(404, "text/html", "<html>not found</html>")
            );

            assertThatThrownBy(() -> scriptedClient.fetch(1102))
                    .isInstanceOf(LottoApiClientException.class);
            assertThat(scriptedClient.fetchCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("제이슨이 아닌 본문은 예외가 발생한다")
        void fetchThrowsOnNonJsonBody() {
            ScriptedDhLotteryApiClient scriptedClient = new ScriptedDhLotteryApiClient(
                    2,
                    new DhLotteryApiClient.ApiRawResponse(200, "text/plain", "<html>oops</html>")
            );

            assertThatThrownBy(() -> scriptedClient.fetch(1102))
                    .isInstanceOf(LottoApiClientException.class);
            assertThat(scriptedClient.fetchCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("미추첨 미래 회차 에이치티엠엘 응답은 빈 옵셔널을 반환한다")
        void fetchReturnsEmptyOnHtmlBodyForFutureRound() {
            ScriptedDhLotteryApiClient scriptedClient = new ScriptedDhLotteryApiClient(
                    2,
                    new DhLotteryApiClient.ApiRawResponse(200, "text/html", "<html>not drawn</html>")
            );

            assertThat(scriptedClient.fetch(9999)).isEmpty();
            assertThat(scriptedClient.fetchCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 추첨됐어야 할 회차에 에이치티엠엘 응답이 오면 예외가 발생한다")
        void fetchThrowsOnHtmlBodyForExpectedRound() {
            ScriptedDhLotteryApiClient scriptedClient = new ScriptedDhLotteryApiClient(
                    0,
                    new DhLotteryApiClient.ApiRawResponse(200, "text/html", "<html>blocked</html>")
            );

            assertThatThrownBy(() -> scriptedClient.fetch(1))
                    .isInstanceOf(LottoApiClientException.class)
                    .extracting(e -> ((LottoApiClientException) e).getFailureReason())
                    .isEqualTo(LottoApiClientException.FailureReason.HTML_UPSTREAM_BLOCKED);
        }

        @Test
        @DisplayName("미추첨 에이치티엠엘 응답 수신 시 반열림 상태가 닫힘로 전환된다")
        void halfOpenClosesWhenNotDrawnHtmlIsReturned() {
            AtomicLong now = new AtomicLong(0L);
            ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 1, 1000, 1, now::get);
            breaker.tryAcquirePermission();
            breaker.recordFailure();
            assertThat(breaker.stateName()).isEqualTo("open");
            now.addAndGet(TimeUnit.MILLISECONDS.toNanos(1000));

            ScriptedDhLotteryApiClient scriptedClient = new ScriptedDhLotteryApiClient(
                    0,
                    breaker,
                    new DhLotteryApiClient.ApiRawResponse(200, "text/html", "<html>not drawn</html>")
            );

            assertThat(scriptedClient.fetch(9999)).isEmpty();
            assertThat(breaker.stateName()).isEqualTo("closed");
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
            ScriptedDhLotteryApiClient scriptedClient = new ScriptedDhLotteryApiClient(
                    2,
                    retrySupport,
                    new LottoApiClientException("network")
            );

            assertThatThrownBy(() -> scriptedClient.fetch(1102))
                    .isInstanceOf(ApiRequestTimeoutException.class);

            assertThat(sleeps).containsExactly(100L);
            assertThat(scriptedClient.fetchCalls()).isEqualTo(1);
        }
    }

    private static final class ScriptedDhLotteryApiClient extends DhLotteryApiClient {

        private final List<Object> scripted;
        private int fetchCalls;

        ScriptedDhLotteryApiClient(int maxRetries, Object... scripted) {
            super(
                    null,
                    new ObjectMapper(),
                    "http://localhost",
                    maxRetries,
                    new SimpleMeterRegistry(),
                    Clock.systemDefaultZone(),
                    new ApiRetrySupport(0, 0),
                    ApiCircuitBreaker.disabled()
            );
            this.scripted = new ArrayList<>(List.of(scripted));
        }

        ScriptedDhLotteryApiClient(int maxRetries, ApiRetrySupport retrySupport, Object... scripted) {
            super(
                    null,
                    new ObjectMapper(),
                    "http://localhost",
                    maxRetries,
                    new SimpleMeterRegistry(),
                    Clock.systemDefaultZone(),
                    retrySupport,
                    ApiCircuitBreaker.disabled()
            );
            this.scripted = new ArrayList<>(List.of(scripted));
        }

        ScriptedDhLotteryApiClient(int maxRetries, ApiCircuitBreaker circuitBreaker, Object... scripted) {
            super(
                    null,
                    new ObjectMapper(),
                    "http://localhost",
                    maxRetries,
                    new SimpleMeterRegistry(),
                    Clock.systemDefaultZone(),
                    new ApiRetrySupport(0, 0),
                    circuitBreaker
            );
            this.scripted = new ArrayList<>(List.of(scripted));
        }

        @Override
        ApiRawResponse doFetch(int round) {
            fetchCalls++;
            if (scripted.isEmpty()) {
                throw new LottoApiClientException("no scripted response");
            }
            Object next = scripted.remove(0);
            if (next instanceof RuntimeException ex) {
                throw ex;
            }
            return (ApiRawResponse) next;
        }

        int fetchCalls() {
            return fetchCalls;
        }
    }

    private static String successBody(int round) {
        return """
                {%n\
                  "totSellamnt": 79760843000,%n\
                  "returnValue": "success",%n\
                  "drwNoDate": "2024-01-06",%n\
                  "firstWinamnt": 2596477500,%n\
                  "drwtNo6": 33,%n\
                  "drwtNo4": 24,%n\
                  "drwtNo5": 28,%n\
                  "bnusNo": 38,%n\
                  "firstPrzwnerCo": 11,%n\
                  "drwtNo2": 13,%n\
                  "drwtNo3": 23,%n\
                  "drwtNo1": 6,%n\
                  "drwNo": %d%n\
                }%n\
                """.formatted(round);
    }
}
