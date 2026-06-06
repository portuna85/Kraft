package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
