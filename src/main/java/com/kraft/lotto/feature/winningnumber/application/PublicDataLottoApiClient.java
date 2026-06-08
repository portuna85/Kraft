package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 공공데이터포털 로또 당첨번호 API 클라이언트.
 * LottoApiClient의 fallback/enrich 역할.
 * API: https://apis.data.go.kr/B551014/OnlineRtl/getLottoDetails
 */
public class PublicDataLottoApiClient implements LottoApiClient {

    private static final Logger log = LoggerFactory.getLogger(PublicDataLottoApiClient.class);
    private static final String DEFAULT_PATH = "/B551014/OnlineRtl/getLottoDetails";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public PublicDataLottoApiClient(RestClient restClient, ObjectMapper objectMapper,
                                    String baseUrl, String apiKey,
                                    MeterRegistry meterRegistry) {
        this(restClient, objectMapper, baseUrl, apiKey, meterRegistry, Clock.systemDefaultZone());
    }

    PublicDataLottoApiClient(RestClient restClient, ObjectMapper objectMapper,
                             String baseUrl, String apiKey,
                             MeterRegistry meterRegistry, Clock clock) {
        this.restClient    = restClient;
        this.objectMapper  = objectMapper;
        this.baseUrl       = baseUrl;
        this.apiKey        = apiKey;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
        this.clock         = clock;
    }

    @Override
    public Optional<WinningNumber> fetch(int round) {
        long started = System.nanoTime();
        count("kraft.api.public_data.lotto.call.total");
        try {
            String body = doFetch(round);
            if (body == null || body.isBlank()) {
                count("kraft.api.public_data.lotto.call.failure", "reason", "blank_body");
                throw new LottoApiClientException("public-data API returned blank body: round=" + round,
                        LottoApiClientException.FailureReason.BLANK_BODY);
            }
            Optional<WinningNumber> result = parse(round, body);
            if (result.isEmpty()) {
                count("kraft.api.public_data.lotto.call.failure", "reason", "not_drawn");
            } else {
                count("kraft.api.public_data.lotto.call.success");
            }
            return result;
        } catch (LottoApiClientException ex) {
            count("kraft.api.public_data.lotto.call.failure", "reason", ex.metricReason());
            throw ex;
        } catch (RestClientException ex) {
            count("kraft.api.public_data.lotto.call.failure", "reason", "network");
            throw new LottoApiClientException(
                    "public-data lotto API call failed (round=" + round + "): " + ex.getMessage(),
                    ex, LottoApiClientException.FailureReason.NETWORK);
        } finally {
            meterRegistry.timer("kraft.api.public_data.lotto.latency")
                    .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    String doFetch(int round) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl + DEFAULT_PATH)
                .queryParam("serviceKey", apiKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1)
                .queryParam("_type", "json")
                .queryParam("drwNo", round)
                .build(true)
                .toUri();
        return restClient.get().uri(uri).retrieve().body(String.class);
    }

    Optional<WinningNumber> parse(int round, String body) {
        try {
            JsonNode root       = objectMapper.readTree(body);
            if (isNotDrawn(round, root)) {
                return Optional.empty();
            }

            JsonNode item = findItem(root).orElse(null);
            if (item == null) {
                return Optional.empty();
            }
            int drwNo = item.path("drwNo").asInt(0);
            if (drwNo != round) {
                throw new LottoApiClientException(
                        "public-data round mismatch: expected=" + round + ", got=" + drwNo,
                        LottoApiClientException.FailureReason.VALIDATION);
            }

            String dateStr = item.path("drwNoDate").asText(null);
            if (dateStr == null || dateStr.isBlank()) {
                throw new LottoApiClientException(
                        "public-data missing drwNoDate: round=" + round,
                        LottoApiClientException.FailureReason.MISSING_FIELD);
            }
            return Optional.of(toWinningNumber(round, item, body, dateStr));
        } catch (LottoApiClientException ex) {
            throw ex;
        } catch (JsonProcessingException | DateTimeParseException ex) {
            throw new LottoApiClientException(
                    "public-data lotto parse failed: round=" + round + ": " + ex.getMessage(),
                    ex, LottoApiClientException.FailureReason.JSON_PARSE);
        }
    }

    private boolean isNotDrawn(int round, JsonNode root) {
        String resultCode = root.path("response").path("header").path("resultCode").asText("");
        if ("03".equals(resultCode)) {
            log.debug("public-data lotto: round={} not yet drawn (resultCode=03)", round);
            return true;
        }
        if (!"00".equals(resultCode)) {
            throw new LottoApiClientException(
                    "public-data API non-success resultCode=" + resultCode + ": round=" + round,
                    LottoApiClientException.FailureReason.HTTP_ERROR);
        }
        return false;
    }

    private static Optional<JsonNode> findItem(JsonNode root) {
        JsonNode item = root.path("response").path("body").path("items").path("item");
        if (item.isMissingNode() || item.isNull()) {
            return Optional.empty();
        }
        if (!item.isArray()) {
            return Optional.of(item);
        }
        if (item.isEmpty() || item.get(0) == null || item.get(0).isNull()) {
            return Optional.empty();
        }
        return Optional.of(item.get(0));
    }

    private WinningNumber toWinningNumber(int round, JsonNode item, String body, String dateStr) {
        LocalDate drawDate = LocalDate.parse(dateStr, DATE_FORMATTER);
        LottoCombination combination = LottoCombination.of(
                item.path("drwtNo1").asInt(0),
                item.path("drwtNo2").asInt(0),
                item.path("drwtNo3").asInt(0),
                item.path("drwtNo4").asInt(0),
                item.path("drwtNo5").asInt(0),
                item.path("drwtNo6").asInt(0)
        );

        return new WinningNumber(
                round, drawDate, combination, item.path("bnusNo").asInt(0),
                item.path("firstWinamnt").asLong(0L),
                item.path("firstPrzwnerCo").asInt(0),
                item.path("totSellamnt").asLong(0L),
                item.path("firstAccumamnt").asLong(0L),
                item.path("secondWinamnt").asLong(0L),
                item.path("secondPrzwnerCo").asInt(0),
                body, LocalDateTime.now(clock));
    }

    private void count(String metric, String... tags) {
        meterRegistry.counter(metric, tags).increment();
    }
}
