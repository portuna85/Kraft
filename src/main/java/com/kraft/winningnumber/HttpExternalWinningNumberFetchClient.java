package com.kraft.winningnumber;

import com.kraft.common.config.ExternalLottoProperties;
import com.kraft.common.error.ApiException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpExternalWinningNumberFetchClient implements ExternalWinningNumberFetchClient {

    private static final Logger log = LoggerFactory.getLogger(HttpExternalWinningNumberFetchClient.class);

    private final RestClient restClient;
    private final ExternalLottoProperties externalLottoProperties;
    private final ExternalWinningNumberPayloadMapper payloadMapper;

    public HttpExternalWinningNumberFetchClient(ExternalLottoProperties externalLottoProperties,
                                                ExternalWinningNumberPayloadMapper payloadMapper) {
        this.externalLottoProperties = externalLottoProperties;
        this.payloadMapper = payloadMapper;
        var factory = new JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    @CircuitBreaker(name = "externalLotto", fallbackMethod = "fetchRoundFallback")
    public WinningNumberUpsertRequest fetchRound(int round) {
        if (!externalLottoProperties.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LOTTO_SOURCE_DISABLED", "외부 수집 URL이 설정되지 않았습니다.");
        }

        String url = externalLottoProperties.urlTemplate().replace("{round}", Integer.toString(round));
        // 전체 URL은 쿼리 파라미터를 포함할 수 있어 로그에 남기지 않는다 — round만으로 추적에 충분하다.
        log.info("외부 회차 수집 요청 시작: round={}", round);

        Map<String, Object> body = restClient.get()
                .uri(url)
                // Required by dhlottery.co.kr's new API (lt645/selectPstLt645InfoNew.do)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("X-Requested-With", externalLottoProperties.requestedWith())
                .header("Referer", externalLottoProperties.referer())
                .retrieve()
                .body(Map.class);

        if (body == null || body.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_EMPTY", "외부 수집 응답이 비어 있습니다.");
        }

        Map<String, Object> payload = extractPayloadForRound(body, round);
        WinningNumberUpsertRequest request = payloadMapper.toRequest(payload);
        log.info("외부 회차 수집 요청 완료: round={} drawDate={}", request.round(), request.drawDate());
        return request;
    }

    // B-5: circuit breaker open → propagate as BAD_GATEWAY so callers handle uniformly
    @SuppressWarnings("unused")
    private WinningNumberUpsertRequest fetchRoundFallback(int round, CallNotPermittedException ex) {
        log.warn("외부 수집 서킷브레이커 OPEN: round={}", round);
        throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_CIRCUIT_OPEN",
                "외부 수집 서킷브레이커가 열려 있습니다. 잠시 후 다시 시도하세요.");
    }

    // Handles both the new { data: { list: [...] } } envelope and the old flat response.
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayloadForRound(Map<String, Object> body, int round) {
        Object dataObj = body.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap)) {
            return body;
        }
        Object listObj = ((Map<String, Object>) dataMap).get("list");
        if (!(listObj instanceof List<?> list)) {
            return body;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> itemMap) {
                Map<String, Object> itemData = (Map<String, Object>) itemMap;
                Object ltEpsd = itemData.get("ltEpsd");
                if (ltEpsd != null) {
                    int itemRound;
                    try {
                        itemRound = Integer.parseInt(ltEpsd.toString().trim());
                    } catch (NumberFormatException e) {
                        throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_PARSE_ERROR",
                                "회차 번호 파싱 실패: " + ltEpsd);
                    }
                    if (itemRound == round) {
                        return itemData;
                    }
                }
            }
        }
        throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_ROUND_NOT_FOUND",
                "응답 목록에서 회차 %d를 찾을 수 없습니다.".formatted(round));
    }
}
