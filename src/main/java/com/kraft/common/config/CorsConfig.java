package com.kraft.common.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Value("${kraft.public-base-url:}")
    private String publicBaseUrl;

    // 허용 출처 목록: kraft.public-base-url이 설정된 경우 그 값만 허용.
    // 미설정 시 "*" 폴백(로컬 개발용). prod 프로파일에서는 ProdEnvironmentValidator가
    // kraft.public-base-url 필수 설정을 강제하므로 프로덕션에서 "*"가 사용되지 않는다.
    List<String> resolvedOrigins() {
        return publicBaseUrl != null && !publicBaseUrl.isBlank()
                ? List.of(publicBaseUrl)
                : List.of("*");
    }

    @Bean
    CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(resolvedOrigins());
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
