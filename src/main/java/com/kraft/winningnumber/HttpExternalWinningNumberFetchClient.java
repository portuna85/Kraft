package com.kraft.winningnumber;

import com.kraft.common.config.ExternalLottoProperties;
import com.kraft.common.error.ApiException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpExternalWinningNumberFetchClient implements ExternalWinningNumberFetchClient {

    private static final Logger log = LoggerFactory.getLogger(HttpExternalWinningNumberFetchClient.class);

    private final RestClient restClient = RestClient.builder().build();
    private final ExternalLottoProperties externalLottoProperties;
    private final ExternalWinningNumberPayloadMapper payloadMapper;

    public HttpExternalWinningNumberFetchClient(ExternalLottoProperties externalLottoProperties,
                                                ExternalWinningNumberPayloadMapper payloadMapper) {
        this.externalLottoProperties = externalLottoProperties;
        this.payloadMapper = payloadMapper;
    }

    @Override
    public WinningNumberUpsertRequest fetchRound(int round) {
        if (!externalLottoProperties.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LOTTO_SOURCE_DISABLED", "외부 수집 URL이 설정되지 않았습니다.");
        }

        String url = externalLottoProperties.urlTemplate().replace("{round}", Integer.toString(round));
        log.info("외부 회차 수집 요청 시작: round={} url={}", round, url);

        Map<String, Object> payload = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

        if (payload == null || payload.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_EMPTY", "외부 수집 응답이 비어 있습니다.");
        }

        WinningNumberUpsertRequest request = payloadMapper.toRequest(payload);
        log.info("외부 회차 수집 요청 완료: round={} drawDate={}", request.round(), request.drawDate());
        return request;
    }
}
