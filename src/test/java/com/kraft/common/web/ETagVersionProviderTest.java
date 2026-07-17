package com.kraft.common.web;

import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumbersCollectedEvent;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("이태그 버전 제공자 테스트")
class ETagVersionProviderTest {

    @Test
    @DisplayName("최신성 경로는 회차가 알려진 상태에서도 항상 폴백 이태그를 사용한다")
    void freshnessPath_alwaysReturnsNullEvenWithKnownRound() {
        ETagVersionProvider provider = providerWithLatestRound(1234);

        assertThat(provider.etagForPath("/api/v1/rounds/freshness")).isNull();
    }

    @Test
    @DisplayName("장애 이력 경로는 회차가 알려진 상태에서도 항상 폴백 이태그를 사용한다")
    void incidentsPath_alwaysReturnsNullEvenWithKnownRound() {
        ETagVersionProvider provider = providerWithLatestRound(1234);

        assertThat(provider.etagForPath("/api/v1/status/incidents")).isNull();
    }

    @Test
    @DisplayName("회차를 아직 모르는 상태에서도 특수 경로는 폴백 이태그를 사용한다")
    void unknownRoundState_stillReturnsNullForSpecialPaths() {
        ETagVersionProvider provider = providerWithNoRound();

        assertThat(provider.etagForPath("/api/v1/rounds/freshness")).isNull();
        assertThat(provider.etagForPath("/api/v1/status/incidents")).isNull();
    }

    @Test
    @DisplayName("과거 회차 상세 경로는 보정 반영을 위해 항상 폴백 이태그를 사용한다")
    void historicalRoundPath_alwaysReturnsNullForMd5Fallback() {
        ETagVersionProvider provider = providerWithLatestRound(1234);

        assertThat(provider.etagForPath("/api/v1/rounds/5678")).isNull();
    }

    @Test
    @DisplayName("그 외 경로는 최신 회차 기반 이태그를 반환한다")
    void otherNonSpecialPath_stillReturnsMutableRoundEtag() {
        ETagVersionProvider provider = providerWithLatestRound(1234);

        assertThat(provider.etagForPath("/api/v1/rounds/latest")).startsWith("\"round-1234-b");
    }

    @Test
    @DisplayName("과거 회차 재수집 이벤트가 발생해도 mutableETag는 과거 값으로 회귀하지 않는다")
    void onCollected_pastRoundRecollection_doesNotRegressMutableEtag() {
        WinningNumberRepository repository = mock(WinningNumberRepository.class);
        WinningNumber latest = winningNumber(1234);
        when(repository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(latest));
        ETagVersionProvider provider = new ETagVersionProvider(repository);
        provider.init();

        String beforeRegression = provider.etagForPath("/api/v1/rounds/latest");

        // 과거 회차(1200) 재수집 이벤트 — 최신 회차는 여전히 1234이므로 리스너가 조회한 최신값이 쓰인다
        provider.onCollected(new WinningNumbersCollectedEvent(1200, true));

        String afterOldRoundEvent = provider.etagForPath("/api/v1/rounds/latest");
        assertThat(afterOldRoundEvent).startsWith("\"round-1234-b");
        assertThat(afterOldRoundEvent).isNotEqualTo(beforeRegression);
        assertThat(afterOldRoundEvent).doesNotContain("round-1200");
    }

    private static WinningNumber winningNumber(int round) {
        return new WinningNumber(
                round,
                LocalDate.of(2002, 12, 7),
                10, 23, 29, 33, 37, 40,
                16,
                857_956_000L,
                0L, 0, 0L, 0L,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        );
    }

    private static ETagVersionProvider providerWithLatestRound(int round) {
        WinningNumberRepository repository = mock(WinningNumberRepository.class);
        WinningNumber winningNumber = new WinningNumber(
                round,
                LocalDate.of(2002, 12, 7),
                10, 23, 29, 33, 37, 40,
                16,
                857_956_000L,
                0L, 0, 0L, 0L,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        );
        when(repository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(winningNumber));
        ETagVersionProvider provider = new ETagVersionProvider(repository);
        provider.init();
        return provider;
    }

    private static ETagVersionProvider providerWithNoRound() {
        WinningNumberRepository repository = mock(WinningNumberRepository.class);
        when(repository.findTopByOrderByRoundDesc()).thenReturn(Optional.empty());
        ETagVersionProvider provider = new ETagVersionProvider(repository);
        provider.init();
        return provider;
    }
}
