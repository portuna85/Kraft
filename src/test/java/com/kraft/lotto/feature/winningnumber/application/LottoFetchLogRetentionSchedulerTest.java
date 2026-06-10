package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("수집 로그 보존 스케줄러 테스트")
class LottoFetchLogRetentionSchedulerTest {

    @Mock
    LottoFetchLogRepository fetchLogRepository;

    @Test
    @DisplayName("만료된 로그를 배치 단위로 모두 삭제한다")
    void purgesInBatches() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);
        LottoFetchLogRetentionScheduler scheduler =
                new LottoFetchLogRetentionScheduler(fetchLogRepository, fixedClock, collectProperties(90, 2));
        when(fetchLogRepository.findIdsByFetchedAtBefore(any(), any(Pageable.class)))
                .thenReturn(List.of(1L, 2L))
                .thenReturn(List.of(3L))
                .thenReturn(List.of());

        scheduler.purgeExpiredLogs();

        verify(fetchLogRepository).deleteAllByIdInBatch(List.of(1L, 2L));
        verify(fetchLogRepository).deleteAllByIdInBatch(List.of(3L));
    }

    @Test
    @DisplayName("기준 시각는 현재 시각에서 보존 기간을 뺀 값이다")
    void cutoffIsNowMinusRetentionDays() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-01T03:30:00Z"), ZoneOffset.UTC);
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(fetchLogRepository.findIdsByFetchedAtBefore(captor.capture(), any(Pageable.class)))
                .thenReturn(List.of());

        new LottoFetchLogRetentionScheduler(fetchLogRepository, fixedClock, collectProperties(90, 1000)).purgeExpiredLogs();

        assertThat(captor.getValue()).isEqualTo(LocalDateTime.of(2026, 3, 3, 3, 30, 0));
    }

    @Test
    @DisplayName("만료된 로그가 없으면 아무것도 하지 않는다")
    void noOpWhenNothingToDelete() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);
        LottoFetchLogRetentionScheduler scheduler =
                new LottoFetchLogRetentionScheduler(fetchLogRepository, fixedClock, collectProperties(90, 1000));
        when(fetchLogRepository.findIdsByFetchedAtBefore(any(), any(Pageable.class)))
                .thenReturn(List.of());

        scheduler.purgeExpiredLogs();

        verify(fetchLogRepository, never()).deleteAllByIdInBatch(any());
    }

    private static KraftCollectProperties collectProperties(int days, int deleteBatchSize) {
        return new KraftCollectProperties(
                52,
                2000,
                true,
                new KraftCollectProperties.Auto(true, "Asia/Seoul"),
                new KraftCollectProperties.LogRetention(true, days, deleteBatchSize, "0 30 3 * * *")
        );
    }
}
