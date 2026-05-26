package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectStatusResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("로또 수집 명령 서비스")
class LottoCollectionCommandServiceTest {

    @Mock
    WinningNumberRepository winningNumberRepository;

    @Mock
    LottoSingleDrawCollector singleDrawCollector;

    @Mock
    LottoRangeCollector rangeCollector;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("다음 회차를 수집하고 요약 이벤트를 발행한다")
    void collectNextIfNeededPublishesEvent() {
        LottoCollectionCommandService service = service();
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(10));
        when(singleDrawCollector.collectOne(11, false)).thenReturn(CollectResponse.ofInserted(1, 11));

        CollectResponse response = service.collectNextIfNeeded();

        assertThat(response.collected()).isEqualTo(1);
        WinningNumbersCollectedEvent event = publishedEvent();
        assertThat(event.collected()).isEqualTo(1);
        assertThat(event.updated()).isZero();
        assertThat(event.dataChanged()).isTrue();
    }

    @Test
    @DisplayName("최신 회차까지 수집 시 API가 아직 추첨되지 않은 회차를 보고하면 중단한다")
    void collectAllUntilLatestStopsOnNotDrawn() {
        LottoCollectionCommandService service = service();
        when(winningNumberRepository.findMaxRound())
                .thenReturn(Optional.of(0))
                .thenReturn(Optional.of(0))
                .thenReturn(Optional.of(1));
        when(singleDrawCollector.collectOne(1, false)).thenReturn(CollectResponse.ofInserted(1, 1));
        when(singleDrawCollector.collectOne(2, false)).thenReturn(CollectResponse.ofNotDrawn(1));

        CollectResponse response = service.collectAllUntilLatest();

        assertThat(response.collected()).isEqualTo(1);
        assertThat(response.failed()).isZero();
        assertThat(response.latestRound()).isEqualTo(1);
        verify(singleDrawCollector).collectOne(1, false);
        verify(singleDrawCollector).collectOne(2, false);
        verify(singleDrawCollector, never()).collectOne(3, false);
        assertThat(publishedEvent().collected()).isEqualTo(1);
    }

    @Test
    @DisplayName("저장된 회차가 없으면 누락 회차 수집을 진행하지 않는다")
    void collectMissingOnceDoesNothingWhenEmpty() {
        LottoCollectionCommandService service = service();
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.empty());

        CollectResponse response = service.collectMissingOnce();

        assertThat(response.collected()).isZero();
        assertThat(response.latestRound()).isZero();
        verifyNoInteractions(rangeCollector, eventPublisher);
    }

    @Test
    @DisplayName("누락된 회차에 대해서만 수집을 위임한다")
    void collectMissingOnceDelegatesMissingRounds() {
        LottoCollectionCommandService service = service();
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(5));
        when(winningNumberRepository.findRoundsBetween(1, 5)).thenReturn(Set.of(1, 3, 5));
        when(rangeCollector.collectRange(anyList(), eq(false), eq(true)))
                .thenReturn(CollectResponse.ofUpdated(1, 5));

        CollectResponse response = service.collectMissingOnce();

        assertThat(response.updated()).isEqualTo(1);
        verify(rangeCollector).collectRange(argThat(rounds -> rounds.equals(List.of(2, 4))), eq(false), eq(true));
        assertThat(publishedEvent().updated()).isEqualTo(1);
    }

    @Test
    @DisplayName("설정된 maxPerRun에 도달하면 추가 수집을 중단한다")
    void collectAllUntilLatestStopsAtConfiguredMaxPerRun() {
        LottoCollectionCommandService service = new LottoCollectionCommandService(
                winningNumberRepository,
                singleDrawCollector,
                rangeCollector,
                eventPublisher,
                0,
                1,
                2000,
                true
        );
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(10));
        when(singleDrawCollector.collectOne(11, false)).thenReturn(CollectResponse.ofInserted(1, 11));

        CollectResponse response = service.collectAllUntilLatest();

        assertThat(response.collected()).isEqualTo(1);
        verify(singleDrawCollector, times(1)).collectOne(11, false);
    }

    @Test
    @DisplayName("stopOnFailure=false면 실패 회차를 기록하고 다음 회차 수집을 계속한다")
    void collectAllUntilLatestContinuesWhenStopOnFailureDisabled() {
        LottoCollectionCommandService service = new LottoCollectionCommandService(
                winningNumberRepository,
                singleDrawCollector,
                rangeCollector,
                eventPublisher,
                0,
                3,
                2000,
                false
        );
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(10));
        when(singleDrawCollector.collectOne(11, false)).thenReturn(CollectResponse.ofFailed(List.of(11), 10, false));
        when(singleDrawCollector.collectOne(12, false)).thenReturn(CollectResponse.ofInserted(1, 12));
        when(singleDrawCollector.collectOne(13, false)).thenReturn(CollectResponse.ofNotDrawn(12));

        CollectResponse response = service.collectAllUntilLatest();

        assertThat(response.failedRounds()).contains(11);
        assertThat(response.collected()).isEqualTo(1);
        verify(singleDrawCollector).collectOne(11, false);
        verify(singleDrawCollector).collectOne(12, false);
        verify(singleDrawCollector).collectOne(13, false);
    }

    @Test
    @DisplayName("다른 수집 작업이 진행 중이면 최신 회차 수집을 건너뛴다")
    void collectAllUntilLatestSkipsWhenAnotherRunIsActive() throws Exception {
        LottoCollectionCommandService service = new LottoCollectionCommandService(
                winningNumberRepository,
                singleDrawCollector,
                rangeCollector,
                eventPublisher,
                0,
                1,
                2000,
                true
        );
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(10));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(singleDrawCollector.collectOne(11, false)).thenAnswer(invocation -> {
            started.countDown();
            if (!release.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("test synchronization timeout");
            }
            return CollectResponse.ofInserted(1, 11);
        });

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<CollectResponse> first = executor.submit(service::collectAllUntilLatest);
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

            CollectResponse skipped = service.collectAllUntilLatest();
            assertThat(skipped.skipped()).isEqualTo(1);
            assertThat(skipped.collected()).isZero();
            assertThat(skipped.updated()).isZero();
            assertThat(skipped.skippedExecution()).isTrue();
            assertThat(skipped.skippedReason()).isEqualTo("overlap");

            release.countDown();
            CollectResponse completed = first.get(2, TimeUnit.SECONDS);
            assertThat(completed.collected()).isEqualTo(1);
        }

        verify(singleDrawCollector, times(1)).collectOne(11, false);
    }

    @Test
    @DisplayName("실행 중에는 active 상태를 반환하고 완료 후 idle로 복귀한다")
    void statusTransitionsFromActiveToIdle() throws Exception {
        LottoCollectionCommandService service = new LottoCollectionCommandService(
                winningNumberRepository,
                singleDrawCollector,
                rangeCollector,
                eventPublisher,
                0,
                1,
                2000,
                true
        );
        when(winningNumberRepository.findMaxRound())
                .thenReturn(Optional.of(10))
                .thenReturn(Optional.of(10));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<CollectStatusResponse> statusDuringRun = new AtomicReference<>();

        when(singleDrawCollector.collectOne(11, false)).thenAnswer(invocation -> {
            statusDuringRun.set(service.getStatus());
            started.countDown();
            if (!release.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("test synchronization timeout");
            }
            return CollectResponse.ofInserted(1, 11);
        });

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<CollectResponse> future = executor.submit(service::collectAllUntilLatest);
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            CollectStatusResponse active = statusDuringRun.get();
            assertThat(active.running()).isTrue();
            assertThat(active.operation()).isEqualTo("collect-all");
            release.countDown();
            future.get(2, TimeUnit.SECONDS);
        }

        CollectStatusResponse idle = service.getStatus();
        assertThat(idle.running()).isFalse();
        assertThat(idle.operation()).isNull();
    }

    private LottoCollectionCommandService service() {
        return new LottoCollectionCommandService(
                winningNumberRepository,
                singleDrawCollector,
                rangeCollector,
                eventPublisher,
                0,
                52,
                2000,
                true
        );
    }

    private WinningNumbersCollectedEvent publishedEvent() {
        ArgumentCaptor<WinningNumbersCollectedEvent> captor = ArgumentCaptor.forClass(WinningNumbersCollectedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }
}
