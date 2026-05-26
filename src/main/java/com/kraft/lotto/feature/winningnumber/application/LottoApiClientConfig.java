package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.winningnumber.domain.LottoDrawSchedule;
import com.kraft.lotto.infra.config.KraftApiProperties;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * {@link LottoApiClient} 구현 선택을 담당한다.
 * <ul>
 *   <li>{@code kraft.api.client=dhlottery} 또는 {@code real} → {@link DhLotteryApiClient}</li>
 *   <li>그 외(기본 포함) → {@link MockLottoApiClient}</li>
 * </ul>
 * 외부 표기({@code real})와 내부 구현체({@code dhlottery})를 동시에 허용하여
 * 환경변수나 배포 매니페스트에서 직관적인 토큰을 그대로 사용할 수 있게 한다.
 */
@Configuration
public class LottoApiClientConfig {

    static final Set<String> DHLOTTERY_TOKENS = Set.of("dhlottery", "real");
    static final Set<String> SMOK_TOKENS = Set.of("smok");

    public static Set<String> prodAllowedClientTokens() {
        return Stream.concat(DHLOTTERY_TOKENS.stream(), SMOK_TOKENS.stream())
                .collect(Collectors.toUnmodifiableSet());
    }
    private final Clock clock;

    @Autowired
    public LottoApiClientConfig(Clock clock) {
        this.clock = clock;
    }

    /** mock latest round를 계산할 수 없는 경우 날짜 기반으로 산출한 예상 회차를 사용한다. */
    int mockDefaultLatestRound() {
        return LottoDrawSchedule.expectedRound(LocalDate.now(clock));
    }

    @Bean
    public RestClient lottoRestClient(KraftApiProperties properties) {
        int connectTimeoutMs = Math.min(properties.connectTimeoutMs(), properties.requestTimeoutMs());
        int readTimeoutMs = Math.min(properties.readTimeoutMs(), properties.requestTimeoutMs());
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        var builder = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01");
        if (properties.userAgent() != null && !properties.userAgent().isBlank()) {
            builder.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent());
        }
        if (properties.acceptLanguage() != null && !properties.acceptLanguage().isBlank()) {
            builder.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, properties.acceptLanguage());
        }
        if (properties.referer() != null && !properties.referer().isBlank()) {
            builder.defaultHeader("Referer", properties.referer());
        }
        return builder.build();
    }

    @Bean
    public LottoApiClient lottoApiClient(KraftApiProperties properties,
                                         RestClient lottoRestClient,
                                         ObjectProvider<ObjectMapper> objectMapperProvider,
                                         ObjectProvider<MeterRegistry> meterRegistryProvider,
                                         ObjectProvider<WinningNumberRepository> winningNumberRepositoryProvider,
                                         ApiCircuitBreakerRegistry circuitBreakerRegistry) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        String client = properties.client() == null ? "" : properties.client().trim().toLowerCase();
        int resolvedMockLatestRound = resolveMockLatestRound(properties, winningNumberRepositoryProvider.getIfAvailable());
        if (DHLOTTERY_TOKENS.contains(client)) {
            ApiCircuitBreaker circuitBreaker = circuitBreakerRegistry.register("dhlottery", apiCircuitBreaker(properties));
            return new DhLotteryApiClient(
                    lottoRestClient,
                    objectMapper,
                    properties.url(),
                    properties.maxRetries(),
                    properties.retryBackoffMs(),
                    properties.requestTimeoutMs(),
                    meterRegistryProvider.getIfAvailable(),
                    circuitBreaker
            );
        }
        if (SMOK_TOKENS.contains(client)) {
            ApiCircuitBreaker circuitBreaker = circuitBreakerRegistry.register("smok", apiCircuitBreaker(properties));
            return new SmokLottoApiClient(
                    lottoRestClient,
                    objectMapper,
                    SmokLottoApiClient.DEFAULT_BASE_URL,
                    properties.maxRetries(),
                    properties.retryBackoffMs(),
                    properties.requestTimeoutMs(),
                    meterRegistryProvider.getIfAvailable(),
                    circuitBreaker
            );
        }
        return new MockLottoApiClient(resolvedMockLatestRound);
    }

    private static ApiCircuitBreaker apiCircuitBreaker(KraftApiProperties properties) {
        return new ApiCircuitBreaker(
                properties.circuitBreakerEnabled(),
                properties.circuitBreakerFailureThreshold(),
                properties.circuitBreakerOpenDurationMs(),
                properties.circuitBreakerHalfOpenMaxCalls()
        );
    }

    private int resolveMockLatestRound(KraftApiProperties properties, WinningNumberRepository repository) {
        if (properties.mockLatestRound() > 0) {
            return properties.mockLatestRound();
        }
        if (repository != null) {
            int latestStoredRound = repository.findMaxRound().orElse(0);
            if (latestStoredRound > 0) {
                return latestStoredRound + 1;
            }
        }
        return mockDefaultLatestRound();
    }
}
