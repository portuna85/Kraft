package com.kraft.common.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Value("${kraft.public-base-url:}")
    private String publicBaseUrl;

    private final Environment env;
    private final ExternalLottoProperties externalLottoProperties;
    private final RevalidateProperties revalidateProperties;
    private final OpsProperties opsProperties;

    public CorsConfig(Environment env,
                       ExternalLottoProperties externalLottoProperties,
                       RevalidateProperties revalidateProperties,
                       OpsProperties opsProperties) {
        this.env = env;
        this.externalLottoProperties = externalLottoProperties;
        this.revalidateProperties = revalidateProperties;
        this.opsProperties = opsProperties;
    }

    /**
     * prod 프로파일에서 이 값들이 비어 있으면 자동수집/ISR 재검증/ops API가 "조용히" 비활성화되어
     * 운영 데이터 신선도 정체로 이어질 수 있다(P0). 그래서 기동 시점에 fail-fast 한다.
     */
    @PostConstruct
    void validate() {
        if (!Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            return;
        }
        List<String> missing = new ArrayList<>();
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            missing.add("KRAFT_PUBLIC_BASE_URL");
        }
        if (externalLottoProperties.urlTemplate() == null || externalLottoProperties.urlTemplate().isBlank()) {
            missing.add("KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE");
        }
        if (revalidateProperties.secret() == null || revalidateProperties.secret().isBlank()) {
            missing.add("KRAFT_REVALIDATE_SECRET");
        }
        if (opsProperties.token() == null || opsProperties.token().isBlank()) {
            missing.add("KRAFT_OPS_TOKEN");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "prod profile requires the following environment variables to be set: " + missing);
        }
    }

    @Bean
    CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // S-1: 환경변수로 출처를 제한. 미설정 시 "*" 폴백(로컬 개발 호환)
        config.setAllowedOriginPatterns(
                publicBaseUrl != null && !publicBaseUrl.isBlank()
                        ? List.of(publicBaseUrl)
                        : List.of("*")
        );
        // B-3: DELETE 추가, X-Device-Token 추가
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Content-Type", "Accept", "Authorization",
                "X-Device-Token", "X-Request-Id"
        ));
        // S-2: X-RateLimit-Reset 미발급이므로 제거, X-RateLimit-Limit 추가
        config.setExposedHeaders(List.of("X-Request-Id", "X-RateLimit-Limit", "X-RateLimit-Remaining"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
