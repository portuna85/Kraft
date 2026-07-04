package com.kraft.common.web;

import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ETagVersionProvider 테스트")
class ETagVersionProviderTest {

    @Test
    @DisplayName("freshness 경로는 회차가 알려진 상태에서도 항상 null(MD5 폴백)을 반환한다")
    void freshnessPath_alwaysReturnsNullEvenWithKnownRound() {
        ETagVersionProvider provider = providerWithLatestRound(1234);

        assertThat(provider.etagForPath("/api/v1/rounds/freshness")).isNull();
    }

    @Test
    @DisplayName("incidents 경로는 회차가 알려진 상태에서도 항상 null(MD5 폴백)을 반환한다")
    void incidentsPath_alwaysReturnsNullEvenWithKnownRound() {
        ETagVersionProvider provider = providerWithLatestRound(1234);

        assertThat(provider.etagForPath("/api/v1/status/incidents")).isNull();
    }

    @Test
    @DisplayName("회차를 아직 모르는 상태에서도 freshness/incidents는 null을 반환한다")
    void unknownRoundState_stillReturnsNullForSpecialPaths() {
        ETagVersionProvider provider = providerWithNoRound();

        assertThat(provider.etagForPath("/api/v1/rounds/freshness")).isNull();
        assertThat(provider.etagForPath("/api/v1/status/incidents")).isNull();
    }

    @Test
    @DisplayName("히스토리 회차 경로는 여전히 round-N ETag를 반환한다")
    void historicalRoundPath_stillReturnsRoundEtag() {
        ETagVersionProvider provider = providerWithLatestRound(1234);

        assertThat(provider.etagForPath("/api/v1/rounds/5678")).isEqualTo("\"round-5678\"");
    }

    @Test
    @DisplayName("그 외 비-특수 경로는 여전히 최신 회차 기반 ETag를 반환한다")
    void otherNonSpecialPath_stillReturnsMutableRoundEtag() {
        ETagVersionProvider provider = providerWithLatestRound(1234);

        assertThat(provider.etagForPath("/api/v1/rounds/latest")).isEqualTo("\"round-1234\"");
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
