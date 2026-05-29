package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.LottoDrawSchedule;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

public class DhLotteryApiClient implements LottoApiClient {

    private static final Logger log = LoggerFactory.getLogger(DhLotteryApiClient.class);

    private final RestClient restClient;
    private final DhLotteryResponseParser responseParser;
    private final String baseUrl;
    private final int maxRetries;
    private final ApiRetrySupport retrySupport;
    private final MeterRegistry meterRegistry;
    private final ApiCircuitBreaker circuitBreaker;
    private final Clock clock;

    private static final Set<String> ALLOWED_FAILURE_REASONS = Set.of(
            "http_error", "blank_body", "non_json", "html_upstream_blocked", "network", "timeout",
            "json_parse", "validation", "transform", "unexpected_return_value", "circuit_open",
            "missing_field", "other"
    );

    public DhLotteryApiClient(RestClient restClient, ObjectMapper objectMapper, String baseUrl) {
        this(restClient, objectMapper, baseUrl, 0, new SimpleMeterRegistry(), Clock.systemDefaultZone(),
                new ApiRetrySupport(0, 0), ApiCircuitBreaker.disabled());
    }

    public DhLotteryApiClient(RestClient restClient,
                              ObjectMapper objectMapper,
                              String baseUrl,
                              int maxRetries,
                              int retryBackoffMs,
                              int requestTimeoutMs,
                              MeterRegistry meterRegistry,
                              ApiCircuitBreaker circuitBreaker) {
        this(restClient, objectMapper, baseUrl, maxRetries, meterRegistry, Clock.systemDefaultZone(),
                new ApiRetrySupport(retryBackoffMs, requestTimeoutMs), circuitBreaker);
    }

    DhLotteryApiClient(RestClient restClient,
                       ObjectMapper objectMapper,
                       String baseUrl,
                       int maxRetries,
                       MeterRegistry meterRegistry,
                       Clock clock,
                       ApiRetrySupport retrySupport,
                       ApiCircuitBreaker circuitBreaker) {
        this.restClient = restClient;
        this.responseParser = new DhLotteryResponseParser(objectMapper, clock);
        this.baseUrl = baseUrl;
        this.maxRetries = Math.max(0, maxRetries);
        this.retrySupport = retrySupport;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = circuitBreaker == null ? ApiCircuitBreaker.disabled() : circuitBreaker;
        this.clock = clock;
    }

    @Override
    public Optional<WinningNumber> fetch(int round) {
        long started = retrySupport.nowNanos();
        count("kraft.api.dhlottery.call.total");
        try {
            return ApiCallExecutor.executeWithRetry(
                    retrySupport,
                    maxRetries,
                    circuitBreaker,
                    timeoutMessage(round),
                    "dhlottery circuit breaker open (round=" + round + ")",
                    deadline -> executeAttempt(round, deadline),
                    (attempt, attempts, deadline, ex) -> handleRestClientException(round, deadline, attempts, attempt, ex),
                    (attempt, attempts, deadline, ex) -> handleClientException(round, deadline, attempts, attempt, ex),
                    () -> count("kraft.api.dhlottery.call.failure", "reason", "circuit_open")
            );
        } finally {
            meterRegistry.timer("kraft.api.dhlottery.latency")
                    .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    private Optional<WinningNumber> executeAttempt(int round, long deadline) {
        ApiRawResponse response = doFetch(round);
        retrySupport.throwIfExpired(deadline, timeoutMessage(round));
        validateResponseStatus(round, response);
        String body = validateResponseBody(round, response);
        if (isHtmlResponse(response)) {
            return handleHtmlResponse(round, response);
        }
        validateJsonResponse(round, response);
        return handleParsedResponse(round, body);
    }

    private void validateResponseStatus(int round, ApiRawResponse response) {
        if (response.statusCode() < 400) {
            return;
        }
        String responseBody = response.body() == null ? "" : response.body();
        count("kraft.api.dhlottery.call.failure", "reason", "http_error");
        throw new LottoApiClientException(
                "external API HTTP error (round=" + round + ", status=" + response.statusCode()
                        + ", preview=" + preview(responseBody) + ")",
                response.statusCode(),
                response.body(),
                LottoApiClientException.FailureReason.HTTP_ERROR
        );
    }

    private String validateResponseBody(int round, ApiRawResponse response) {
        String body = response.body();
        if (body != null && !body.isBlank()) {
            return body;
        }
        count("kraft.api.dhlottery.call.failure", "reason", "blank_body");
        throw new LottoApiClientException(
                "response body is blank (round=" + round + ")",
                response.statusCode(),
                response.body(),
                LottoApiClientException.FailureReason.BLANK_BODY
        );
    }

    private Optional<WinningNumber> handleHtmlResponse(int round, ApiRawResponse response) {
        if (round <= LottoDrawSchedule.expectedRound(LocalDate.now(clock))) {
            count("kraft.api.dhlottery.call.failure", "reason", "html_upstream_blocked");
            throw new LottoApiClientException(
                    "HTML response for expected round=" + round + " (server may be blocking)",
                    response.statusCode(),
                    preview(response.body()),
                    LottoApiClientException.FailureReason.HTML_UPSTREAM_BLOCKED
            );
        }
        log.debug("lotto round not yet drawn (HTML response): round={}", round);
        count("kraft.api.dhlottery.call.empty", "reason", "not_drawn");
        circuitBreaker.recordSuccess();
        return Optional.empty();
    }

    private Optional<WinningNumber> handleParsedResponse(int round, String body) {
        Optional<WinningNumber> parsed = parse(round, body);
        if (parsed.isEmpty()) {
            count("kraft.api.dhlottery.call.empty", "reason", "not_drawn");
        } else {
            count("kraft.api.dhlottery.call.success");
        }
        circuitBreaker.recordSuccess();
        return parsed;
    }

    private void handleRestClientException(int round,
                                           long deadline,
                                           int attempts,
                                           int attempt,
                                           RestClientException ex) {
        count("kraft.api.dhlottery.call.failure", "reason", "network");
        circuitBreaker.recordFailure();
        if (attempt >= attempts) {
            throw new LottoApiClientException(
                    "external API call failed (round=" + round + ", attempts=" + attempts + ")",
                    ex,
                    null,
                    null,
                    LottoApiClientException.FailureReason.NETWORK
            );
        }
        count("kraft.api.dhlottery.call.retry");
        log.warn("dhlottery call failed, retrying: round={}, attempt={}/{}, reason={}",
                round, attempt, attempts, ex.getMessage());
        log.debug("dhlottery retry detail: round={}, attempt={}/{}, cause={}({})",
                round, attempt, attempts, ex.getClass().getSimpleName(), ex.getMessage());
        sleepBackoff(deadline, round);
    }

    private void handleClientException(int round,
                                       long deadline,
                                       int attempts,
                                       int attempt,
                                       LottoApiClientException ex) {
        if (ex instanceof ApiRequestTimeoutException) {
            count("kraft.api.dhlottery.call.failure", "reason", "timeout");
            circuitBreaker.recordFailure();
            throw ex;
        }
        countFailureReasonMetric(ex.metricReason());
        circuitBreaker.recordFailure();
        if (attempt >= attempts || !isRetriable(ex)) {
            throw new LottoApiClientException(
                    "external API call failed (round=" + round + ", attempts=" + attempts + ")",
                    ex,
                    ex.getResponseCode(),
                    ex.getRawResponse(),
                    ex.getFailureReason()
            );
        }
        count("kraft.api.dhlottery.call.retry");
        log.warn("dhlottery call failed, retrying: round={}, attempt={}/{}, reason={}",
                round, attempt, attempts, ex.getMessage());
        log.debug("dhlottery retry detail: round={}, attempt={}/{}, cause={}({})",
                round, attempt, attempts, ex.getClass().getSimpleName(), ex.getMessage());
        sleepBackoff(deadline, round);
    }

    private void countFailureReasonMetric(String reason) {
        if ("json_parse".equals(reason)
                || "validation".equals(reason)
                || "transform".equals(reason)
                || "unexpected_return_value".equals(reason)
                || "non_json".equals(reason)
                || "missing_field".equals(reason)) {
            count("kraft.api.dhlottery.call.failure", "reason", reason);
        }
    }

    ApiRawResponse doFetch(int round) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("method", "getLottoNumber")
                .queryParam("drwNo", round)
                .build()
                .toUri();
        return restClient.get()
                .uri(uri)
                .exchange((request, rawResponse) -> {
                    int statusCode = rawResponse.getStatusCode().value();
                    String body = StreamUtils.copyToString(rawResponse.getBody(), StandardCharsets.UTF_8);
                    MediaType contentType = rawResponse.getHeaders().getContentType();
                    return new ApiRawResponse(statusCode, contentType == null ? null : contentType.toString(), body);
                });
    }

    Optional<WinningNumber> parse(int round, String body) {
        return responseParser.parse(round, body);
    }

    private static boolean isHtmlResponse(ApiRawResponse response) {
        String contentType = response.contentType() == null ? "" : response.contentType().toLowerCase();
        return contentType.contains("text/html");
    }

    private static void validateJsonResponse(int round, ApiRawResponse response) {
        String body = response.body() == null ? "" : response.body().trim();
        String contentType = response.contentType() == null ? "" : response.contentType().toLowerCase();
        boolean jsonContentType = contentType.isBlank() || contentType.contains("json") || contentType.contains("javascript");
        boolean jsonBody = body.startsWith("{") || body.startsWith("[");
        if (!jsonContentType || !jsonBody) {
            throw new LottoApiClientException("response is not JSON (round=" + round
                    + ", contentType=" + response.contentType() + ", preview=" + preview(body) + ")", response.statusCode(),
                    response.body(), LottoApiClientException.FailureReason.NON_JSON);
        }
    }

    private void sleepBackoff(long deadline, int round) {
        retrySupport.sleepBeforeRetry(deadline, timeoutMessage(round), "retry sleep interrupted");
    }

    private void count(String metricName, String... tags) {
        if ("kraft.api.dhlottery.call.failure".equals(metricName) && tags.length >= 2 && "reason".equals(tags[0])) {
            String reason = tags[1];
            if (!ALLOWED_FAILURE_REASONS.contains(reason)) {
                String[] t = tags.clone();
                t[1] = "other";
                meterRegistry.counter(metricName, t).increment();
                return;
            }
        }
        meterRegistry.counter(metricName, tags).increment();
    }

    private static String preview(String body) {
        int limit = Math.min(80, body.length());
        return body.substring(0, limit).replaceAll("\\s+", " ");
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
        return "external API request timeout exceeded (round=" + round + ")";
    }

    record ApiRawResponse(int statusCode, String contentType, String body) {
    }
}
