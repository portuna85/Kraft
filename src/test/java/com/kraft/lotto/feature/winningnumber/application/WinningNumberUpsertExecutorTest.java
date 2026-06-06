package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.kraft.lotto.support.fixtures.LottoTestFixtures.winningNumber;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberMapper;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("당첨번호 upsert 실행기 테스트")
class WinningNumberUpsertExecutorTest {

    private final WinningNumberRepository repository = mock(WinningNumberRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-14T00:00:00Z"), ZoneOffset.UTC);
    private final WinningNumberUpsertExecutor executor = new WinningNumberUpsertExecutor(repository, clock);

    @Test
    @DisplayName("저장된 행이 없으면 INSERTED를 반환한다")
    void returnsInsertedWhenRowAbsent() {
        stubFindById(1201, Optional.empty());
        stubNativeUpsert();

        UpsertOutcome outcome = executor.upsertOnce(sample(1201));

        assertThat(outcome).isEqualTo(UpsertOutcome.INSERTED);
    }

    @Test
    @DisplayName("저장된 행이 있고 데이터가 동일하면 UNCHANGED를 반환한다")
    void returnsUnchangedWhenDataIsSame() {
        WinningNumber wn = sample(1200);
        stubFindById(1200, Optional.of(toEntity(wn)));
        stubNativeUpsert();

        UpsertOutcome outcome = executor.upsertOnce(wn);

        assertThat(outcome).isEqualTo(UpsertOutcome.UNCHANGED);
    }

    @Test
    @DisplayName("저장된 행이 있고 데이터가 다르면 UPDATED를 반환한다")
    void returnsUpdatedWhenDataDiffers() {
        stubFindById(1200, Optional.of(toEntity(sample(1200))));
        stubNativeUpsert();

        UpsertOutcome outcome = executor.upsertOnce(changedSample(1200));

        assertThat(outcome).isEqualTo(UpsertOutcome.UPDATED);
    }

    private void stubFindById(int round, Optional<WinningNumberEntity> result) {
        when(repository.findById(round)).thenReturn(result);
    }

    private void stubNativeUpsert() {
        when(repository.nativeUpsert(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(0);
    }

    private WinningNumberEntity toEntity(WinningNumber wn) {
        return WinningNumberMapper.toEntity(wn, clock.instant().atZone(ZoneOffset.UTC).toLocalDateTime());
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
