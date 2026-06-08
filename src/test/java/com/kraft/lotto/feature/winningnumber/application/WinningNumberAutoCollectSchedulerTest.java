package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("당첨 번호 자동 수집 스케줄러")
class WinningNumberAutoCollectSchedulerTest {

    @Test
    @DisplayName("수집 성공 시 성공 메트릭을 기록한다")
    void recordsSuccessMetrics() {
        LottoCollectionCommandService service = mock(LottoCollectionCommandService.class);
        when(service.collectAllUntilLatest()).thenReturn(CollectResponse.ofInserted(1, 1200));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WinningNumberAutoCollectScheduler scheduler = new WinningNumberAutoCollectScheduler(
                service,
                provider(registry)
        );

        scheduler.collectSaturday2230();

        assertThat(registry.get("kraft.collect.auto.run")
                .tag("trigger", "sat-22-30")
                .tag("status", "success")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.get("kraft.collect.auto.latency")
                .tag("trigger", "sat-22-30")
                .tag("status", "success")
                .timer()
                .count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("수집 중 예외 발생 시 실패 메트릭을 기록한다")
    void recordsFailureMetrics() {
        LottoCollectionCommandService service = mock(LottoCollectionCommandService.class);
        doThrow(new IllegalStateException("boom")).when(service).collectAllUntilLatest();

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WinningNumberAutoCollectScheduler scheduler = new WinningNumberAutoCollectScheduler(
                service,
                provider(registry)
        );

        scheduler.collectSunday0700();

        assertThat(registry.get("kraft.collect.auto.run")
                .tag("trigger", "sun-07-00")
                .tag("status", "failure")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.get("kraft.collect.auto.error")
                .tag("trigger", "sun-07-00")
                .tag("exception", "illegalstateexception")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("일요일 07:00 트리거로 수집 성공 시 메트릭을 기록한다")
    void recordsSuccessMetricsForSunday0700() {
        LottoCollectionCommandService service = mock(LottoCollectionCommandService.class);
        when(service.collectAllUntilLatest()).thenReturn(CollectResponse.ofInserted(1, 1227));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WinningNumberAutoCollectScheduler scheduler = new WinningNumberAutoCollectScheduler(
                service,
                provider(registry)
        );

        scheduler.collectSunday0700();

        assertThat(registry.get("kraft.collect.auto.run")
                .tag("trigger", "sun-07-00")
                .tag("status", "success")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("중복 실행 시 두 번째 스케줄 실행은 건너뛴다")
    void skipsOverlappingRuns() throws Exception {
        LottoCollectionCommandService service = mock(LottoCollectionCommandService.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(service.collectAllUntilLatest()).thenAnswer(invocation -> {
            started.countDown();
            boolean released = release.await(3, TimeUnit.SECONDS);
            assertThat(released).isTrue();
            return CollectResponse.ofInserted(1, 1200);
        });

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WinningNumberAutoCollectScheduler scheduler = new WinningNumberAutoCollectScheduler(
                service,
                provider(registry)
        );

        Thread first = new Thread(scheduler::collectSaturday2230);
        first.start();
        assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();

        scheduler.collectSaturday2230();

        assertThat(registry.get("kraft.collect.auto.run")
                .tag("trigger", "sat-22-30")
                .tag("status", "skipped_overlap")
                .counter()
                .count()).isEqualTo(1.0);

        release.countDown();
        first.join(3000);
    }

    private static ObjectProvider<MeterRegistry> provider(MeterRegistry registry) {
        return new ObjectProvider<>() {
            @Override
            public MeterRegistry getObject(Object... args) {
                return registry;
            }

            @Override
            public MeterRegistry getIfAvailable() {
                return registry;
            }

            @Override
            public MeterRegistry getIfUnique() {
                return registry;
            }
        };
    }
}
