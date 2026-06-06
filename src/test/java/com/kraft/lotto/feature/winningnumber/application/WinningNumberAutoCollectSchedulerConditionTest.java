package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("당첨 번호 자동 수집 스케줄러 활성화 조건 테스트")
class WinningNumberAutoCollectSchedulerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class,
                    TaskSchedulingAutoConfiguration.class
            ))
            .withUserConfiguration(TestConfig.class, SchedulerImportConfig.class);

    @Test
    @DisplayName("스케줄러와 자동 수집 플래그가 모두 활성화되면 빈을 생성한다")
    void createsSchedulerBeanWhenBothFlagsAreEnabled() {
        contextRunner
                .withPropertyValues(
                        "kraft.lotto.scheduler.enabled=true",
                        "kraft.collect.auto.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(WinningNumberAutoCollectScheduler.class));
    }

    @Test
    @DisplayName("인프라 스케줄러가 비활성화되면 빈을 생성하지 않는다")
    void doesNotCreateSchedulerBeanWhenInfraSchedulerDisabled() {
        contextRunner
                .withPropertyValues(
                        "kraft.lotto.scheduler.enabled=false",
                        "kraft.collect.auto.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(WinningNumberAutoCollectScheduler.class));
    }

    @Test
    @DisplayName("자동 수집 플래그가 비활성화되면 빈을 생성하지 않는다")
    void doesNotCreateSchedulerBeanWhenAutoCollectDisabled() {
        contextRunner
                .withPropertyValues(
                        "kraft.lotto.scheduler.enabled=true",
                        "kraft.collect.auto.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(WinningNumberAutoCollectScheduler.class));
    }

    @Test
    @DisplayName("수집 성공 시 성공 메트릭을 기록한다")
    void recordsSuccessMetrics() {
        LottoCollectionCommandService service = Mockito.mock(LottoCollectionCommandService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(service.collectAllUntilLatest()).thenReturn(CollectResponse.ofFailed(List.of(1, 2), 1200, false));
        WinningNumberAutoCollectScheduler scheduler = new WinningNumberAutoCollectScheduler(service, fixedProvider(meterRegistry));

        scheduler.collectSaturday2200();

        assertThat(meterRegistry.get("kraft.collect.auto.run")
                .tag("trigger", "sat-22-00")
                .tag("status", "success")
                .counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("kraft.collect.auto.round.failure")
                .tag("trigger", "sat-22-00")
                .counter().count()).isEqualTo(2.0d);
    }

    @Test
    @DisplayName("수집 실패 시 예외를 전파하지 않고 실패 메트릭을 기록한다")
    void recordsFailureMetricsWithoutPropagating() {
        LottoCollectionCommandService service = Mockito.mock(LottoCollectionCommandService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(service.collectAllUntilLatest()).thenThrow(new IllegalStateException("boom"));
        WinningNumberAutoCollectScheduler scheduler = new WinningNumberAutoCollectScheduler(service, fixedProvider(meterRegistry));

        scheduler.collectSunday0600();

        assertThat(meterRegistry.get("kraft.collect.auto.run")
                .tag("trigger", "sun-06-00")
                .tag("status", "failure")
                .counter().count()).isEqualTo(1.0d);
    }

    @Configuration
    static class TestConfig {

        @Bean
        LottoCollectionCommandService lottoCollectionCommandService() {
            return Mockito.mock(LottoCollectionCommandService.class);
        }

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Configuration
    @Import(WinningNumberAutoCollectScheduler.class)
    static class SchedulerImportConfig {
    }

    private static <T> ObjectProvider<T> fixedProvider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }
        };
    }
}
