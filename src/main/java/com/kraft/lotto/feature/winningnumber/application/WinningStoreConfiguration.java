package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.infra.config.KraftApiProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class WinningStoreConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WinningStoreConfiguration.class);
    private static final String STORE_BASE_URL    = "https://www.dhlottery.co.kr/store.do";
    private static final String SESSION_SEED_URL  = "https://www.dhlottery.co.kr/gameResult.do?method=byWin";

    @Bean
    WinningStoreApiClient winningStoreApiClient(KraftApiProperties properties, ObjectMapper objectMapper,
                                                ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);

        WinningStoreApiClient primaryClient;
        String primaryName;
        if (properties.storeRelayUrl() != null && !properties.storeRelayUrl().isBlank()) {
            log.info("store relay URL configured, using RelayStoreApiClient: {}", properties.storeRelayUrl());
            primaryClient = buildRelayClient(properties, objectMapper);
            primaryName = "relay";
        } else {
            primaryClient = buildDhLotteryClient(properties, objectMapper);
            primaryName = "dhlottery";
        }

        if (properties.publicDataApiKey() != null && !properties.publicDataApiKey().isBlank()) {
            log.info("public-data API key configured, wrapping with CompositeWinningStoreApiClient (fallback=public-data)");
            RestClient publicDataClient = buildPublicDataRestClient(properties);
            String baseUrl = properties.publicDataBaseUrl() != null && !properties.publicDataBaseUrl().isBlank()
                    ? properties.publicDataBaseUrl() : "https://apis.data.go.kr";
            WinningStoreApiClient fallback = new PublicDataStoreApiClient(
                    publicDataClient, objectMapper, baseUrl, properties.publicDataApiKey());
            return new CompositeWinningStoreApiClient(primaryClient, primaryName, fallback, "public-data", meterRegistry);
        }

        log.info("no public-data API key, using single store client: {}", primaryName);
        return primaryClient;
    }

    private WinningStoreApiClient buildDhLotteryClient(KraftApiProperties properties, ObjectMapper objectMapper) {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        int connectMs = Math.min(properties.connectTimeoutMs(), properties.requestTimeoutMs());
        int readMs    = Math.min(properties.readTimeoutMs(),    properties.requestTimeoutMs());

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(cookieManager)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readMs));

        var builder = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01")
                .defaultHeader("X-Requested-With", "XMLHttpRequest");
        if (properties.userAgent() != null && !properties.userAgent().isBlank()) {
            builder.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent());
        }
        if (properties.acceptLanguage() != null && !properties.acceptLanguage().isBlank()) {
            builder.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, properties.acceptLanguage());
        }
        RestClient storeClient = builder.build();

        RestClient tracerClient = buildTracerClient();
        String serverIp = detectServerIp(tracerClient);
        DhLotteryTracerClient tracer = new DhLotteryTracerClient(tracerClient, serverIp);

        return new DhLotteryStoreApiClient(
                storeClient, objectMapper,
                STORE_BASE_URL, SESSION_SEED_URL,
                cookieManager, tracer,
                properties.userAgent() != null ? properties.userAgent() : "");
    }

    private static RestClient buildPublicDataRestClient(KraftApiProperties properties) {
        int connectMs = Math.min(properties.connectTimeoutMs(), properties.requestTimeoutMs());
        int readMs    = Math.min(properties.readTimeoutMs(),    properties.requestTimeoutMs());
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readMs));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    private static WinningStoreApiClient buildRelayClient(KraftApiProperties properties, ObjectMapper objectMapper) {
        int connectMs = Math.min(properties.connectTimeoutMs(), properties.requestTimeoutMs());
        int readMs    = Math.min(properties.readTimeoutMs(),    properties.requestTimeoutMs());
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readMs));
        RestClient relayRestClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
        return new RelayStoreApiClient(relayRestClient, objectMapper, properties.storeRelayUrl());
    }

    private static RestClient buildTracerClient() {
        HttpClient tracerHttp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(tracerHttp);
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().requestFactory(factory).build();
    }

    private static String detectServerIp(RestClient client) {
        try {
            String ip = client.get()
                    .uri("https://api.ipify.org")
                    .retrieve()
                    .body(String.class);
            if (ip != null && !ip.isBlank()) {
                log.info("store tracer: detected server IP={}", ip.trim());
                return ip.trim();
            }
        } catch (Exception e) {
            log.debug("store tracer: server IP detection failed ({}), using empty", e.getMessage());
        }
        return "";
    }
}
