package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

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

    private static final Set<String> ALLOWED_FAILURE_REASONS = Set.of(
            "http_error", "blank_body", "non_json", "html_upstream_blocked", "network", "timeout",
            "json_parse", "validation", "transform", "unexpected_return_value", "circuit_open",
            "missing_field", "other"
    );

    public DhLotteryApiClient(RestClient restClient, ObjectMapper objectMapper, String baseUrl) {
        this(restClient, objectMapper, baseUrl, 0, 0, null, Clock.systemDefaultZone(), ApiCircuitBreaker.disabled());
    }

    public DhLotteryApiClient(RestClient restClient,
                              ObjectMapper objectMapper,
                              String baseUrl,
                              int maxRetries,
                              int retryBackoffMs,
                              MeterRegistry meterRegistry) {
        this(restClient, objectMapper, baseUrl, maxRetries, retryBackoffMs, 0, meterRegistry, ApiCircuitBreaker.disabled());
    }

    public DhLotteryApiClient(RestClient restClient,
                              ObjectMapper objectMapper,
                              String baseUrl,
                              int maxRetries,
                              int retryBackoffMs,
                              int requestTimeoutMs,
                              MeterRegistry meterRegistry) {
        this(restClient, objectMapper, baseUrl, maxRetries, retryBackoffMs, requestTimeoutMs, meterRegistry, ApiCircuitBreaker.disabled());
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
                       int retryBackoffMs,
                       MeterRegistry meterRegistry,
                       Clock clock) {
        this(restClient, objectMapper, baseUrl, maxRetries, retryBackoffMs, meterRegistry, clock, ApiCircuitBreaker.disabled());
    }

    DhLotteryApiClient(RestClient restClient,
                       ObjectMapper objectMapper,
                       String baseUrl,
                       int maxRetries,
                       int retryBackoffMs,
                       MeterRegistry meterRegistry,
                       Clock clock,
                       ApiCircuitBreaker circuitBreaker) {
        this(restClient, objectMapper, baseUrl, maxRetries, meterRegistry, clock,
                new ApiRetrySupport(retryBackoffMs, 0), circuitBreaker);
    }

    DhLotteryApiClient(RestClient restClient,
                       ObjectMapper objectMapper,
                       String baseUrl,
                       int maxRetries,
                       MeterRegistry meterRegistry,
                       Clock clock,
                       ApiRetrySupport retrySupport) {
        this(restClient, objectMapper, baseUrl, maxRetries, meterRegistry, clock, retrySupport, ApiCircuitBreaker.disabled());
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
    }

    @Override
    public Optional<WinningNumber> fetch(int round) {
        long started = retrySupport.nowNanos();
        long deadline = retrySupport.deadlineFrom(started);
        int attempts = maxRetries + 1;
        count("kraft.api.dhlottery.call.total");
        int attempt = 0;
        try {
            while (true) {
                retrySupport.throwIfExpired(deadline, timeoutMessage(round));
                attempt++;
                if (!circuitBreaker.tryAcquirePermission()) {
                    count("kraft.api.dhlottery.call.failure", "reason", "circuit_open");
                    throw new CircuitBreakerOpenException("dhlottery circuit breaker open (round=" + round + ")");
                }
                try {
                    ApiRawResponse response = doFetch(round);
                    retrySupport.throwIfExpired(deadline, timeoutMessage(round));
                    if (response.statusCode() >= 400) {
                        String responseBody = response.body() == null ? "" : response.body();
                        count("kraft.api.dhlottery.call.failure", "reason", "http_error");
                        throw new LottoApiClientException("external API HTTP error (round=" + round + ", status=" + response.statusCode()
                                + ", preview=" + preview(responseBody) + ")", response.statusCode(), response.body(),
                                LottoApiClientException.FailureReason.HTTP_ERROR);
                    }
                    String body = response.body();
                    if (body == null || body.isBlank()) {
                        count("kraft.api.dhlottery.call.failure", "reason", "blank_body");
                        throw new LottoApiClientException("response body is blank (round=" + round + ")",
                                response.statusCode(), response.body(), LottoApiClientException.FailureReason.BLANK_BODY);
                    }
                    if (isHtmlResponse(response)) {
                        if (round <= expectedRoundAsOf(LocalDate.now())) {
                            count("kraft.api.dhlottery.call.failure", "reason", "html_upstream_blocked");
                            throw new LottoApiClientException(
                                    "HTML response for expected round=" + round + " (server may be blocking)",
                                    response.statusCode(), preview(response.body()),
                                    LottoApiClientException.FailureReason.HTML_UPSTREAM_BLOCKED);
                        }
                        log.debug("lotto round not yet drawn (HTML response): round={}", round);
                        count("kraft.api.dhlottery.call.empty", "reason", "not_drawn");
                        circuitBreaker.recordSuccess();
                        return Optional.empty();
                    }
                    validateJsonResponse(round, response);
                    Optional<WinningNumber> parsed = parse(round, body);
                    if (parsed.isEmpty()) {
                        count("kraft.api.dhlottery.call.empty", "reason", "not_drawn");
                    } else {
                        count("kraft.api.dhlottery.call.success");
                    }
                    circuitBreaker.recordSuccess();
                    return parsed;
                } catch (RestClientException ex) {
                    count("kraft.api.dhlottery.call.failure", "reason", "network");
                    circuitBreaker.recordFailure();
                    if (attempt >= attempts) {
                        throw new LottoApiClientException(
                                "external API call failed (round=" + round + ", attempts=" + attempts + ")", ex,
                                null, null, LottoApiClientException.FailureReason.NETWORK);
                    }
                    count("kraft.api.dhlottery.call.retry");
                    log.warn("dhlottery call failed, retrying: round={}, attempt={}/{}, reason={}",
                            round, attempt, attempts, ex.getMessage());
                    log.debug("dhlottery retry detail: round={}, attempt={}/{}, cause={}({})",
                            round, attempt, attempts, ex.getClass().getSimpleName(), ex.getMessage());
                    sleepBackoff(deadline, round);
                } catch (LottoApiClientException ex) {
                    if (ex instanceof ApiRequestTimeoutException) {
                        count("kraft.api.dhlottery.call.failure", "reason", "timeout");
                        circuitBreaker.recordFailure();
                        throw ex;
                    }
                    String reason = ex.metricReason();
                    if ("json_parse".equals(reason)
                            || "validation".equals(reason)
                            || "transform".equals(reason)
                            || "unexpected_return_value".equals(reason)
                            || "non_json".equals(reason)
                            || "missing_field".equals(reason)) {
                        count("kraft.api.dhlottery.call.failure", "reason", reason);
                    }
                    circuitBreaker.recordFailure();
                    if (attempt >= attempts || !isRetriable(ex)) {
                        throw new LottoApiClientException(
                                "external API call failed (round=" + round + ", attempts=" + attempts + ")", ex,
                                ex.getResponseCode(), ex.getRawResponse(), ex.getFailureReason());
                    }
                    count("kraft.api.dhlottery.call.retry");
                    log.warn("dhlottery call failed, retrying: round={}, attempt={}/{}, reason={}",
                            round, attempt, attempts, ex.getMessage());
                    log.debug("dhlottery retry detail: round={}, attempt={}/{}, cause={}({})",
                            round, attempt, attempts, ex.getClass().getSimpleName(), ex.getMessage());
                    sleepBackoff(deadline, round);
                }
            }
        } finally {
            if (meterRegistry != null) {
                meterRegistry.timer("kraft.api.dhlottery.latency")
                        .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
            }
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

    private static final LocalDate FIRST_DRAW_DATE = LocalDate.of(2002, 12, 7);

    private static int expectedRoundAsOf(LocalDate asOf) {
        LocalDate lastSat = asOf.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        if (lastSat.isBefore(FIRST_DRAW_DATE)) {
            return 0;
        }
        return (int) ChronoUnit.WEEKS.between(FIRST_DRAW_DATE, lastSat) + 1;
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
        if (meterRegistry == null) {
            return;
        }
        if ("kraft.api.dhlottery.call.failure".equals(metricName) && tags.length >= 2 && "reason".equals(tags[0])) {
            String reason = tags[1];
            if (!ALLOWED_FAILURE_REASONS.contains(reason)) {
                tags[1] = "other";
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
