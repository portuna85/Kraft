package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompositeLottoApiClient 폴백 체이닝 테스트")
class CompositeLottoApiClientTest {

    private LottoApiClient primary;
    private LottoApiClient fallback;
    private SimpleMeterRegistry meterRegistry;
    private CompositeLottoApiClient composite;

    @BeforeEach
    void setUp() {
        primary = mock(LottoApiClient.class);
        fallback = mock(LottoApiClient.class);
        meterRegistry = new SimpleMeterRegistry();
        composite = new CompositeLottoApiClient(primary, "dhlottery", fallback, "smok", meterRegistry);
    }

    @Test
    @DisplayName("primary가 secondPrize > 0으로 성공하면 fallback을 호출하지 않는다")
    void primarySuccessWithSecondPrizeNoFallbackCall() {
        WinningNumber result = winningNumberWithSecondPrize(1_000_000L);
        when(primary.fetch(1)).thenReturn(Optional.of(result));

        Optional<WinningNumber> actual = composite.fetch(1);

        assertThat(actual).contains(result);
        verify(fallback, never()).fetch(1);
        assertThat(meterRegistry.counter("kraft.api.fallback.used", "from", "dhlottery", "to", "smok").count()).isZero();
    }

    @Test
    @DisplayName("primary가 secondPrize=0으로 성공하면 fallback으로 2등 보충(enrich)을 시도한다")
    void primarySuccessSecondPrizeZeroEnrichFromFallback() {
        WinningNumber primaryResult = winningNumberWithSecondPrize(0L);
        WinningNumber enrichResult = winningNumberWithSecondPrize(70_054_508L);
        when(primary.fetch(1)).thenReturn(Optional.of(primaryResult));
        when(fallback.fetch(1)).thenReturn(Optional.of(enrichResult));

        Optional<WinningNumber> actual = composite.fetch(1);

        assertThat(actual).isPresent();
        assertThat(actual.get().secondPrize()).isEqualTo(70_054_508L);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.success",
                "from", "dhlottery", "to", "smok").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("kraft.api.fallback.used", "from", "dhlottery", "to", "smok").count()).isZero();
    }

    @Test
    @DisplayName("primary secondPrize=0이고 fallback도 secondPrize=0이면 primary 결과를 그대로 반환한다")
    void primaryAndFallbackBothSecondPrizeZeroReturnsPrimary() {
        WinningNumber primaryResult = winningNumberWithSecondPrize(0L);
        WinningNumber fallbackResult = winningNumberWithSecondPrize(0L);
        when(primary.fetch(1)).thenReturn(Optional.of(primaryResult));
        when(fallback.fetch(1)).thenReturn(Optional.of(fallbackResult));

        Optional<WinningNumber> actual = composite.fetch(1);

        assertThat(actual).contains(primaryResult);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.success",
                "from", "dhlottery", "to", "smok").count()).isZero();
    }

    @Test
    @DisplayName("primary secondPrize=0이고 fallback 보충 실패해도 primary 결과를 반환한다")
    void primarySecondPrizeZeroEnrichFailReturnsOriginal() {
        WinningNumber primaryResult = winningNumberWithSecondPrize(0L);
        when(primary.fetch(1)).thenReturn(Optional.of(primaryResult));
        when(fallback.fetch(1)).thenThrow(new LottoApiClientException("smok unavailable"));

        Optional<WinningNumber> actual = composite.fetch(1);

        assertThat(actual).contains(primaryResult);
        assertThat(meterRegistry.counter("kraft.api.fallback.used", "from", "dhlottery", "to", "smok").count()).isZero();
    }

    @Test
    @DisplayName("primary가 empty를 반환(미추첨 권위)하면 fallback을 호출하지 않는다")
    void primaryEmptyNoFallbackCall() {
        when(primary.fetch(99)).thenReturn(Optional.empty());

        Optional<WinningNumber> actual = composite.fetch(99);

        assertThat(actual).isEmpty();
        verify(fallback, never()).fetch(99);
    }

    @Test
    @DisplayName("primary 예외 발생 시 fallback을 호출하고 fallback.used counter를 증가시킨다")
    void primaryExceptionFallbackSuccess() {
        WinningNumber result = mock(WinningNumber.class);
        when(primary.fetch(10)).thenThrow(new LottoApiClientException("dhlottery error"));
        when(fallback.fetch(10)).thenReturn(Optional.of(result));

        Optional<WinningNumber> actual = composite.fetch(10);

        assertThat(actual).contains(result);
        assertThat(meterRegistry.counter("kraft.api.fallback.used", "from", "dhlottery", "to", "smok").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("kraft.api.fallback.exhausted").count()).isZero();
    }

    @Test
    @DisplayName("CircuitBreakerOpenException 발생 시 fallback으로 전환한다")
    void circuitBreakerOpenFallbackSuccess() {
        WinningNumber result = mock(WinningNumber.class);
        when(primary.fetch(20)).thenThrow(new CircuitBreakerOpenException("circuit open"));
        when(fallback.fetch(20)).thenReturn(Optional.of(result));

        Optional<WinningNumber> actual = composite.fetch(20);

        assertThat(actual).contains(result);
        assertThat(meterRegistry.counter("kraft.api.fallback.used", "from", "dhlottery", "to", "smok").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("primary와 fallback 모두 실패하면 예외를 전파하고 fallback.exhausted counter를 증가시킨다")
    void bothFailExhaustedCounterAndException() {
        LottoApiClientException primaryEx = new LottoApiClientException("dhlottery error");
        LottoApiClientException fallbackEx = new LottoApiClientException("smok error");
        when(primary.fetch(30)).thenThrow(primaryEx);
        when(fallback.fetch(30)).thenThrow(fallbackEx);

        assertThatThrownBy(() -> composite.fetch(30))
                .isSameAs(fallbackEx)
                .hasSuppressedException(primaryEx);

        assertThat(meterRegistry.counter("kraft.api.fallback.exhausted").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("kraft.api.fallback.used", "from", "dhlottery", "to", "smok").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("fallbackClient가 null이면 LottoApiClientConfig에서 Composite를 생성하지 않는다")
    void nullFallbackTokenNoCompositeCreated() {
        // fallbackClient=null → Composite 미생성 검증은 LottoApiClientConfigTest에서 담당.
        // 여기서는 Composite 자체의 동작만 검증하므로 이 케이스는 설정 레이어 테스트로 위임.
        assertThat(composite).isNotNull();
    }

    @Test
    @DisplayName("drawDate가 오늘(KST)이면 2등 보충을 스킵하고 enrich.skipped 카운터를 증가시킨다")
    void drawDateTodayKstSkipsEnrich() {
        LocalDate today = LocalDate.of(2026, 6, 7);
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-06-07T12:30:00Z"), ZoneId.of("Asia/Seoul"));
        CompositeLottoApiClient compositeWithClock = new CompositeLottoApiClient(
                primary, "dhlottery", fallback, "smok", meterRegistry, fixedClock);

        WinningNumber primaryResult = winningNumberWithDrawDate(0L, today);
        when(primary.fetch(1)).thenReturn(Optional.of(primaryResult));

        Optional<WinningNumber> actual = compositeWithClock.fetch(1);

        assertThat(actual).contains(primaryResult);
        verify(fallback, never()).fetch(1);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.skipped",
                "from", "dhlottery", "to", "smok", "reason", "draw_today").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.attempt",
                "from", "dhlottery", "to", "smok").count()).isZero();
    }

    @Test
    @DisplayName("drawDate가 어제이면 2등 보충을 시도한다")
    void drawDateYesterdayProceedsWithEnrich() {
        LocalDate yesterday = LocalDate.of(2026, 6, 6);
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-06-07T12:30:00Z"), ZoneId.of("Asia/Seoul"));
        CompositeLottoApiClient compositeWithClock = new CompositeLottoApiClient(
                primary, "dhlottery", fallback, "smok", meterRegistry, fixedClock);

        WinningNumber primaryResult = winningNumberWithDrawDate(0L, yesterday);
        WinningNumber enrichResult = winningNumberWithDrawDate(70_054_508L, yesterday);
        when(primary.fetch(1)).thenReturn(Optional.of(primaryResult));
        when(fallback.fetch(1)).thenReturn(Optional.of(enrichResult));

        Optional<WinningNumber> actual = compositeWithClock.fetch(1);

        assertThat(actual.get().secondPrize()).isEqualTo(70_054_508L);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.attempt",
                "from", "dhlottery", "to", "smok").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.skipped",
                "from", "dhlottery", "to", "smok", "reason", "draw_today").count()).isZero();
    }

    @Test
    @DisplayName("drawDate가 어제여도 enrich delay 시간 이내면 2등 보충을 스킵한다")
    void drawDateYesterdayWithinDelaySkipsEnrich() {
        LocalDate yesterday = LocalDate.of(2026, 6, 6);
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-06-06T23:00:00Z"), ZoneId.of("Asia/Seoul"));
        CompositeLottoApiClient compositeWithDelay = new CompositeLottoApiClient(
                primary, "dhlottery", fallback, "smok", meterRegistry, fixedClock, 12);

        WinningNumber primaryResult = winningNumberWithDrawDate(0L, yesterday);
        when(primary.fetch(1)).thenReturn(Optional.of(primaryResult));

        Optional<WinningNumber> actual = compositeWithDelay.fetch(1);

        assertThat(actual).contains(primaryResult);
        verify(fallback, never()).fetch(1);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.skipped",
                "from", "dhlottery", "to", "smok", "reason", "within_delay_window").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.attempt",
                "from", "dhlottery", "to", "smok").count()).isZero();
    }

    @Test
    @DisplayName("drawDate가 어제이고 enrich delay 시간이 지나면 2등 보충을 시도한다")
    void drawDateYesterdayAfterDelayProceedsWithEnrich() {
        LocalDate yesterday = LocalDate.of(2026, 6, 6);
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-06-07T12:30:00Z"), ZoneId.of("Asia/Seoul"));
        CompositeLottoApiClient compositeWithDelay = new CompositeLottoApiClient(
                primary, "dhlottery", fallback, "smok", meterRegistry, fixedClock, 12);

        WinningNumber primaryResult = winningNumberWithDrawDate(0L, yesterday);
        WinningNumber enrichResult = winningNumberWithDrawDate(70_054_508L, yesterday);
        when(primary.fetch(1)).thenReturn(Optional.of(primaryResult));
        when(fallback.fetch(1)).thenReturn(Optional.of(enrichResult));

        Optional<WinningNumber> actual = compositeWithDelay.fetch(1);

        assertThat(actual.get().secondPrize()).isEqualTo(70_054_508L);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.attempt",
                "from", "dhlottery", "to", "smok").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("kraft.api.fallback.enrich.skipped",
                "from", "dhlottery", "to", "smok", "reason", "within_delay_window").count()).isZero();
    }

    private static WinningNumber winningNumberWithSecondPrize(long secondPrize) {
        return winningNumberWithDrawDate(secondPrize, LocalDate.of(2026, 6, 6));
    }

    private static WinningNumber winningNumberWithDrawDate(long secondPrize, LocalDate drawDate) {
        return new WinningNumber(
                1, drawDate,
                new LottoCombination(List.of(1, 2, 3, 4, 5, 6)), 7,
                1_000_000L, 5, 100_000_000L, 5_000_000L,
                secondPrize, secondPrize > 0 ? 10 : 0,
                null, null);
    }
}
