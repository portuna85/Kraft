package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.kraft.lotto.support.fixtures.LottoTestFixtures.winningNumber;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

@DisplayName("당첨번호 저장기 테스트")
class WinningNumberPersisterTest {

    private final WinningNumberUpsertExecutor executor = mock(WinningNumberUpsertExecutor.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-14T00:00:00Z"), ZoneOffset.UTC);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final WinningNumberPersister persister = new WinningNumberPersister(executor, meterRegistry);

    @Test
    @DisplayName("upsert는 동일 데이터면 UNCHANGED를 반환한다")
    void upsertReturnsUnchangedForSameData() {
        when(executor.upsertOnce(any())).thenReturn(UpsertOutcome.UNCHANGED);

        UpsertOutcome outcome = persister.upsert(sample(1200));

        assertThat(outcome).isEqualTo(UpsertOutcome.UNCHANGED);
    }

    @Test
    @DisplayName("upsert는 기존 데이터가 다르면 UPDATED를 반환한다")
    void upsertReturnsUpdatedWhenDataChanged() {
        when(executor.upsertOnce(any())).thenReturn(UpsertOutcome.UPDATED);

        UpsertOutcome outcome = persister.upsert(changedSample(1200));

        assertThat(outcome).isEqualTo(UpsertOutcome.UPDATED);
    }

    @Test
    @DisplayName("upsert는 신규 데이터면 INSERTED를 반환한다")
    void upsertReturnsInsertedWhenMissing() {
        when(executor.upsertOnce(any())).thenReturn(UpsertOutcome.INSERTED);

        UpsertOutcome outcome = persister.upsert(sample(1201));

        assertThat(outcome).isEqualTo(UpsertOutcome.INSERTED);
    }

    @Test
    @DisplayName("upsert는 신규 저장 충돌 시 UNCHANGED를 반환한다")
    void upsertReturnsUnchangedOnInsertConflict() {
        doThrow(new DataIntegrityViolationException("dup")).when(executor).upsertOnce(any());

        UpsertOutcome outcome = persister.upsert(sample(1202));

        assertThat(outcome).isEqualTo(UpsertOutcome.UNCHANGED);
    }

    @Test
    @DisplayName("upsert는 낙관적 락 충돌 시 재시도 후 성공하면 UPDATED를 반환한다")
    void upsertRetriesOnOptimisticLockAndSucceeds() {
        when(executor.upsertOnce(any()))
                .thenThrow(new OptimisticLockingFailureException("conflict"))
                .thenReturn(UpsertOutcome.UPDATED);

        UpsertOutcome outcome = persister.upsert(changedSample(1200));

        assertThat(outcome).isEqualTo(UpsertOutcome.UPDATED);
    }

    @Test
    @DisplayName("upsert는 낙관적 락 충돌이 재시도 한도를 넘으면 FAILED를 반환한다")
    void upsertReturnsFailedWhenOptimisticLockRetriesExhausted() {
        when(executor.upsertOnce(any()))
                .thenThrow(new OptimisticLockingFailureException("conflict-1"))
                .thenThrow(new OptimisticLockingFailureException("conflict-2"));

        UpsertOutcome outcome = persister.upsert(sample(1200));

        assertThat(outcome).isEqualTo(UpsertOutcome.FAILED);
        assertThat(meterRegistry.get("kraft.winningnumber.optimistic_lock.failure").counter().count()).isEqualTo(1.0);
    }

    private WinningNumber sample(int round) {
        return winningNumber(round, LocalDate.of(2026, 5, 3), new LottoCombination(List.of(6, 13, 23, 24, 28, 33)), 38);
    }

    private WinningNumber changedSample(int round) {
        return new WinningNumber(
                round,
                LocalDate.of(2026, 5, 10),
                new LottoCombination(List.of(1, 2, 3, 4, 5, 6)),
                7,
                3_000_000_000L,
                9,
                80_000_000_000L,
                30_000_000_000L,
                "{\"returnValue\":\"success\"}",
                null
        );
    }
}
