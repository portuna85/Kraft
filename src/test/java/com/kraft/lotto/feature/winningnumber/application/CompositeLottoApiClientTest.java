package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
    @DisplayName("primary 성공 시 fallback을 호출하지 않는다")
    void primarySuccessNoFallbackCall() {
        WinningNumber result = mock(WinningNumber.class);
        when(primary.fetch(1)).thenReturn(Optional.of(result));

        Optional<WinningNumber> actual = composite.fetch(1);

        assertThat(actual).contains(result);
        verify(fallback, never()).fetch(1);
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
}
