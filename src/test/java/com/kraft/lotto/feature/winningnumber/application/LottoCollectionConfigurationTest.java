package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.infra.config.KraftApiProperties;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("LottoCollectionConfiguration 빈 생성 테스트")
class LottoCollectionConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(LottoCollectionConfiguration.class, MockDepsConfig.class);

    @Test
    @DisplayName("LottoSingleDrawCollector 빈이 생성된다")
    void lottoSingleDrawCollectorBeanIsCreated() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(LottoSingleDrawCollector.class));
    }

    @Test
    @DisplayName("LottoRangeCollector 빈이 생성된다")
    void lottoRangeCollectorBeanIsCreated() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(LottoRangeCollector.class));
    }

    @Test
    @DisplayName("LottoCollectionCommandService 빈이 생성된다")
    void lottoCollectionCommandServiceBeanIsCreated() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(LottoCollectionCommandService.class));
    }

    @Configuration
    static class MockDepsConfig {

        @Bean
        LottoApiClient lottoApiClient() {
            return mock(LottoApiClient.class);
        }

        @Bean
        WinningNumberRepository winningNumberRepository() {
            return mock(WinningNumberRepository.class);
        }

        @Bean
        WinningNumberPersister winningNumberPersister() {
            return mock(WinningNumberPersister.class);
        }

        @Bean
        LottoFetchLogRepository lottoFetchLogRepository() {
            return mock(LottoFetchLogRepository.class);
        }

        @Bean
        KraftApiProperties kraftApiProperties() {
            return new KraftApiProperties("mock", "http://localhost", 1000, 1000, 10000, 0, 0, 0, 0, 0,
                    true, 5, 30_000, 1, null, null, null, null, null, null, null);
        }

        @Bean
        KraftCollectProperties kraftCollectProperties() {
            return new KraftCollectProperties(
                    52,
                    2000,
                    true,
                    new KraftCollectProperties.Auto(true, "Asia/Seoul"),
                    new KraftCollectProperties.LogRetention(true, 90, 1000, "0 30 3 * * *")
            );
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return mock(ApplicationEventPublisher.class);
        }
    }
}
