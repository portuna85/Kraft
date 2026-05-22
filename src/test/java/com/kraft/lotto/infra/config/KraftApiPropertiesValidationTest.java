package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

@DisplayName("로또 API 설정 프로퍼티 검증 테스트")
class KraftApiPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "kraft.api.client=mock",
                    "kraft.api.url=http://localhost",
                    "kraft.api.connect-timeout-ms=2000",
                    "kraft.api.read-timeout-ms=3000",
                    "kraft.api.request-timeout-ms=10000",
                    "kraft.api.max-retries=0",
                    "kraft.api.retry-backoff-ms=0",
                    "kraft.api.mock-latest-round=0"
            );

    @Test
    @DisplayName("음수 connect-timeout-ms는 바인딩 실패를 유발한다")
    void rejectsNegativeConnectTimeout() {
        runner.withPropertyValues("kraft.api.connect-timeout-ms=-1")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    @DisplayName("request-timeout-ms는 양수여야 한다")
    void rejectsNonPositiveRequestTimeout() {
        runner.withPropertyValues("kraft.api.request-timeout-ms=0")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    @DisplayName("blank client는 바인딩 실패를 유발한다")
    void rejectsBlankClient() {
        runner.withPropertyValues("kraft.api.client=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    @DisplayName("url이 비어있으면 바인딩에 실패한다")
    void rejectsBlankUrl() {
        runner.withPropertyValues("kraft.api.url=")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    @DisplayName("비정상적으로 큰 타임아웃이나 재시도 값은 바인딩에 실패한다")
    void rejectsValuesOverOperationalMax() {
        runner.withPropertyValues(
                        "kraft.api.request-timeout-ms=60001",
                        "kraft.api.max-retries=11",
                        "kraft.api.backfill-delay-ms=60001"
                )
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(KraftApiProperties.class)
    static class TestConfig {
    }
}
