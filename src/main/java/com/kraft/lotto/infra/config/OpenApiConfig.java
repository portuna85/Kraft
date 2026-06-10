package com.kraft.lotto.infra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kraftLottoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("KRAFT Lotto API")
                        .description("로또 6/45 당첨번호 수집·분석·추천 REST API. "
                                + "X-Api-Key 헤더 인증은 서버 설정에 따라 선택적으로 적용됩니다.")
                        .version("v1")
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Api-Key")
                                .description("kraft.api.key.enabled=true 시 필수. "
                                        + "KRAFT_API_KEY_KEYS 환경변수로 유효 키 목록을 설정합니다.")));
    }

    @Bean
    public GroupedOpenApi publicApiGroup() {
        return GroupedOpenApi.builder()
                .group("public-v1")
                .displayName("Public API v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi opsApiGroup() {
        return GroupedOpenApi.builder()
                .group("ops")
                .displayName("Ops API")
                .pathsToMatch("/ops/**")
                .build();
    }
}
