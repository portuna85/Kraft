package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.kraft.lotto.support.fixtures.LottoTestFixtures.winningNumber;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("당첨번호 upsert 실행기 테스트")
class WinningNumberUpsertExecutorTest {

    private final WinningNumberRepository repository = mock(WinningNumberRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-14T00:00:00Z"), ZoneOffset.UTC);
    private final WinningNumberUpsertExecutor executor = new WinningNumberUpsertExecutor(repository, clock);

    @Test
    @DisplayName("동일 데이터면 UNCHANGED를 반환하고 saveAndFlush를 호출하지 않는다")
    void returnsUnchangedForSameData() {
        WinningNumber wn = sample(1200);
        when(repository.findById(1200)).thenReturn(Optional.of(entityFrom(wn)));

        UpsertOutcome outcome = executor.upsertOnce(wn);

        assertThat(outcome).isEqualTo(UpsertOutcome.UNCHANGED);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("데이터가 다르면 UPDATED를 반환하고 엔티티를 갱신한다")
    void returnsUpdatedWhenDataChanged() {
        WinningNumber existing = sample(1200);
        WinningNumber incoming = new WinningNumber(
                1200,
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
        WinningNumberEntity existingEntity = entityFrom(existing);
        when(repository.findById(1200)).thenReturn(Optional.of(existingEntity));

        UpsertOutcome outcome = executor.upsertOnce(incoming);

        assertThat(outcome).isEqualTo(UpsertOutcome.UPDATED);
        assertThat(existingEntity.getN1()).isEqualTo(1);
        assertThat(existingEntity.getBonusNumber()).isEqualTo(7);
    }

    @Test
    @DisplayName("신규 데이터면 INSERTED를 반환하고 saveAndFlush를 호출한다")
    void returnsInsertedWhenMissing() {
        WinningNumber wn = sample(1201);
        when(repository.findById(1201)).thenReturn(Optional.empty());

        UpsertOutcome outcome = executor.upsertOnce(wn);

        assertThat(outcome).isEqualTo(UpsertOutcome.INSERTED);
        ArgumentCaptor<WinningNumberEntity> captor = ArgumentCaptor.forClass(WinningNumberEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRound()).isEqualTo(1201);
    }

    private WinningNumber sample(int round) {
        return winningNumber(round, LocalDate.of(2026, 5, 3), new LottoCombination(List.of(6, 13, 23, 24, 28, 33)), 38);
    }

    private WinningNumberEntity entityFrom(WinningNumber wn) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        return new WinningNumberEntity(
                wn.round(),
                wn.drawDate(),
                wn.combination().numbers().get(0),
                wn.combination().numbers().get(1),
                wn.combination().numbers().get(2),
                wn.combination().numbers().get(3),
                wn.combination().numbers().get(4),
                wn.combination().numbers().get(5),
                wn.bonusNumber(),
                wn.firstPrize(),
                wn.firstWinners(),
                wn.totalSales(),
                wn.firstAccumAmount(),
                wn.rawJson(),
                now,
                now,
                now
        );
    }
}
