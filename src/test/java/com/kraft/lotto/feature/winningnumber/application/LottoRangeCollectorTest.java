package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse;
import com.kraft.lotto.support.BusinessException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("로또 범위 수집기")
class LottoRangeCollectorTest {

    @Mock
    LottoSingleDrawCollector singleDrawCollector;

    @Mock
    WinningNumberRepository winningNumberRepository;

    @Test
    @DisplayName("각 회차별 수집 결과를 집계한다")
    void aggregatesSingleRoundResults() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LottoRangeCollector collector = new LottoRangeCollector(singleDrawCollector, winningNumberRepository, 0, registry);
        when(singleDrawCollector.collectOne(1, false)).thenReturn(CollectResponse.ofInserted(1, 10));
        when(singleDrawCollector.collectOne(2, false)).thenReturn(CollectResponse.ofUpdated(1, 11));
        when(singleDrawCollector.collectOne(3, false)).thenReturn(CollectResponse.ofFailed(List.of(3), 11, false));
        when(winningNumberRepository.findMaxRound()).thenReturn(Optional.of(11));

        CollectResponse response = collector.collectRange(List.of(1, 2, 3), false, true);

        assertThat(response.collected()).isEqualTo(1);
        assertThat(response.updated()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.failedRounds()).containsExactly(3);
        assertThat(response.latestRound()).isEqualTo(11);
        assertThat(registry.get("kraft.collect.range.rounds").summary().totalAmount()).isEqualTo(3.0);
        assertThat(registry.get("kraft.collect.range.failed").summary().totalAmount()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("대기 시간 인터럽트 발생 시 비즈니스 예외로 변환한다")
    void interruptedBackfillDelayThrowsBusinessException() {
        LottoRangeCollector collector = new LottoRangeCollector(singleDrawCollector, winningNumberRepository, 60_000, null);
        when(singleDrawCollector.collectOne(1, false)).thenReturn(CollectResponse.ofSkipped(1, 1));

        Thread.currentThread().interrupt();
        try {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> collector.collectRange(List.of(1, 2), false, true));

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(singleDrawCollector).collectOne(1, false);
            verify(singleDrawCollector, never()).collectOne(2, false);
        } finally {
            Thread.interrupted();
        }
    }
}
