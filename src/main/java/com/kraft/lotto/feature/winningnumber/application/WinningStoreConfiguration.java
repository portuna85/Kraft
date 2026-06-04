package com.kraft.lotto.feature.winningnumber.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.infra.config.KraftApiProperties;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class WinningStoreConfiguration {

    private static final String STORE_BASE_URL = "https://www.dhlottery.co.kr/store.do";
    private static final String SESSION_SEED_URL = "https://www.dhlottery.co.kr/gameResult.do?method=byWin";

    @Bean
    WinningStoreApiClient winningStoreApiClient(KraftApiProperties properties, ObjectMapper objectMapper) {
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
        return new DhLotteryStoreApiClient(builder.build(), objectMapper, STORE_BASE_URL, SESSION_SEED_URL);
    }
}
