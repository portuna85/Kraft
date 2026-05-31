package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * smok95.github.io/lotto GitHub Pages 기반 당첨번호 클라이언트.
 * dhlottery.co.kr 트레이서 차단 우회용.
 * 엔드포인트: https://smok95.github.io/lotto/results/{round}.json
 */
public class SmokLottoApiClient implements LottoApiClient {

    private static final Logger log = LoggerFactory.getLogger(SmokLottoApiClient.class);

    static final String DEFAULT_BASE_URL = "https://smok95.github.io/lotto/results";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Clock clock;
    private final int maxRetries;
    private final ApiRetrySupport retrySupport;
    private final MeterRegistry meterRegistry;
    private final ApiCircuitBreaker circuitBreaker;

    public SmokLottoApiClient(RestClient restClient, ObjectMapper objectMapper, String baseUrl,
                              int maxRetries, int retryBackoffMs, int requestTimeoutMs, MeterRegistry meterRegistry,
                              ApiCircuitBreaker circuitBreaker) {
        this(restClient, objectMapper, baseUrl, maxRetries, meterRegistry, Clock.systemDefaultZone(),
                new ApiRetrySupport(retryBackoffMs, requestTimeoutMs), circuitBreaker);
    }

    SmokLottoApiClient(RestClient restClient, ObjectMapper objectMapper, String baseUrl, Clock clock) {
        this(restClient, objectMapper, baseUrl, 0, new SimpleMeterRegistry(), clock,
                new ApiRetrySupport(0, 0), ApiCircuitBreaker.disabled());
    }

    SmokLottoApiClient(RestClient restClient, ObjectMapper objectMapper, String baseUrl,
                       int maxRetries, MeterRegistry meterRegistry, Clock clock, ApiRetrySupport retrySupport,
                       ApiCircuitBreaker circuitBreaker) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.clock = clock;
        this.maxRetries = Math.max(0, maxRetries);
        this.retrySupport = retrySupport;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = circuitBreaker == null ? ApiCircuitBreaker.disabled() : circuitBreaker;
    }

    @Override
    public Optional<WinningNumber> fetch(int round) {
        long started = retrySupport.nowNanos();
        count("kraft.api.smok.call.total");
        try {
            return ApiCallExecutor.executeWithRetry(
                    retrySupport,
                    maxRetries,
                    circuitBreaker,
                    timeoutMessage(round),
                    "smok circuit breaker open (round=" + round + ")",
                    deadline -> executeAttempt(round, deadline),
                    (attempt, attempts, deadline, ex) -> handleRestClientException(round, deadline, attempts, attempt, ex),
                    (attempt, attempts, deadline, ex) -> handleClientException(round, deadline, attempts, attempt, ex),
                    () -> count("kraft.api.smok.call.failure", "reason", "circuit_open")
            );
        } finally {
            meterRegistry.timer("kraft.api.smok.latency")
                    .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    RawResult doFetch(URI uri) {
        return restClient.get()
                .uri(uri)
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    return new RawResult(status, body);
                });
    }

    private Optional<WinningNumber> executeAttempt(int round, long deadline) {
        URI uri = URI.create(baseUrl + "/" + round + ".json");
        RawResult raw = doFetch(uri);
        retrySupport.throwIfExpired(deadline, timeoutMessage(round));
        if (raw.statusCode() == 404) {
            count("kraft.api.smok.call.empty", "reason", "not_drawn");
            circuitBreaker.recordSuccess();
            return Optional.empty();
        }
        if (raw.statusCode() >= 400) {
            count("kraft.api.smok.call.failure", "reason", "http_error");
            throw new LottoApiClientException(
                    "smok API HTTP error (round=" + round + ", status=" + raw.statusCode() + ")",
                    raw.statusCode(), raw.body());
        }
        Optional<WinningNumber> parsed = parse(round, raw.body());
        count("kraft.api.smok.call.success");
        circuitBreaker.recordSuccess();
        return parsed;
    }

    Optional<WinningNumber> parse(int round, String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            int drawNo = requiredInt(node, "draw_no");
            if (drawNo != round) {
                throw new LottoApiClientException(
                        "round mismatch: request=" + round + ", response=" + drawNo);
            }

            List<Integer> mains = requiredMainNumbers(node);
            int bonus = requiredInt(node, "bonus_no");
            LocalDate drawDate = requiredDate(node, "date");
            long totalSales = node.path("total_sales_amount").asLong(0L);

            JsonNode div1 = node.path("divisions").path(0);
            long firstPrize = div1.path("prize").asLong(0L);
            int firstWinners = div1.path("winners").asInt(0);
            long firstAccumAmount = firstPrize * firstWinners;

            return Optional.of(new WinningNumber(
                    round, drawDate, new LottoCombination(mains), bonus,
                    firstPrize, firstWinners, totalSales, firstAccumAmount, body, LocalDateTime.now(clock)));
        } catch (LottoApiClientException ex) {
            throw ex;
        } catch (Exception ex) {
            count("kraft.api.smok.call.failure", "reason", "parse_error");
            throw new LottoApiClientException(
                    "smok API parse failed (round=" + round + "): " + ex.getMessage(), ex);
        }
    }

    private int requiredInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            count("kraft.api.smok.call.failure", "reason", "missing_field");
            throw new LottoApiClientException("missing required field: " + fieldName);
        }
        if (!value.canConvertToInt()) {
            count("kraft.api.smok.call.failure", "reason", "missing_field");
            throw new LottoApiClientException("invalid integer field: " + fieldName);
        }
        return value.asInt();
    }

    private List<Integer> requiredMainNumbers(JsonNode node) {
        JsonNode numbersNode = node.path("numbers");
        if (numbersNode.isMissingNode() || numbersNode.isNull() || !numbersNode.isArray()) {
            count("kraft.api.smok.call.failure", "reason", "missing_field");
            throw new LottoApiClientException("missing required field: numbers");
        }
        if (numbersNode.size() != 6) {
            count("kraft.api.smok.call.failure", "reason", "invalid_numbers");
            throw new LottoApiClientException("invalid numbers size: " + numbersNode.size());
        }
        List<Integer> mains = new ArrayList<>(6);
        for (int i = 0; i < numbersNode.size(); i++) {
            JsonNode value = numbersNode.get(i);
            if (value == null || !value.canConvertToInt()) {
                count("kraft.api.smok.call.failure", "reason", "invalid_numbers");
                throw new LottoApiClientException("invalid numbers[" + i + "]");
            }
            mains.add(value.asInt());
        }
        return mains;
    }

    private LocalDate requiredDate(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            count("kraft.api.smok.call.failure", "reason", "missing_field");
            throw new LottoApiClientException("missing required field: " + fieldName);
        }
        try {
            return OffsetDateTime.parse(value.asText()).toLocalDate();
        } catch (Exception ex) {
            count("kraft.api.smok.call.failure", "reason", "date_parse");
            throw new LottoApiClientException("invalid date field: " + fieldName, ex);
        }
    }

    private void sleepBackoff(long deadline, int round) {
        retrySupport.sleepBeforeRetry(deadline, timeoutMessage(round), "retry sleep interrupted");
    }

    private void handleRestClientException(int round,
                                           long deadline,
                                           int attempts,
                                           int attempt,
                                           RestClientException ex) {
        count("kraft.api.smok.call.failure", "reason", "network");
        circuitBreaker.recordFailure();
        if (attempt >= attempts) {
            throw new LottoApiClientException(
                    "smok API call failed (round=" + round + ", attempts=" + attempts + ")", ex);
        }
        count("kraft.api.smok.call.retry");
        log.warn("smok call failed, retrying: round={}, attempt={}/{}, reason={}",
                round, attempt, attempts, ex.getMessage());
        sleepBackoff(deadline, round);
    }

    private void handleClientException(int round,
                                       long deadline,
                                       int attempts,
                                       int attempt,
                                       LottoApiClientException ex) {
        if (ex instanceof ApiRequestTimeoutException) {
            count("kraft.api.smok.call.failure", "reason", "timeout");
            circuitBreaker.recordFailure();
            throw ex;
        }
        circuitBreaker.recordFailure();
        if (attempt >= attempts || !isRetriable(ex)) {
            throw new LottoApiClientException(
                    "smok API call failed (round=" + round + ", attempts=" + attempts + ")", ex);
        }
        count("kraft.api.smok.call.retry");
        log.warn("smok call failed, retrying: round={}, attempt={}/{}, reason={}",
                round, attempt, attempts, ex.getMessage());
        sleepBackoff(deadline, round);
    }

    private void count(String metricName, String... tags) {
        meterRegistry.counter(metricName, tags).increment();
    }

    private static boolean isRetriable(LottoApiClientException ex) {
        if (ex instanceof ApiRequestTimeoutException || ex instanceof CircuitBreakerOpenException) {
            return false;
        }
        Integer code = ex.getResponseCode();
        if (code == null) {
            return true;
        }
        return code == 429 || code >= 500;
    }

    private static String timeoutMessage(int round) {
        return "smok API request timeout exceeded (round=" + round + ")";
    }

    record RawResult(int statusCode, String body) {
    }
}
