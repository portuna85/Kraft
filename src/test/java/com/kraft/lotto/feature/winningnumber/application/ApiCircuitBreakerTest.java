package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("API 서킷 브레이커")
class ApiCircuitBreakerTest {

    @Test
    @DisplayName("연속 실패 시 서킷이 열리고 일정 시간 후 회복된다")
    void opensAndRecovers() {
        AtomicLong now = new AtomicLong(0L);
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(
                true,
                2,
                1000,
                1,
                now::get
        );

        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.recordFailure();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.recordFailure();

        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertThat(breaker.stateName()).isEqualTo("open");

        now.addAndGet(TimeUnit.MILLISECONDS.toNanos(1000));
        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.stateName()).isEqualTo("half_open");

        breaker.recordSuccess();
        assertThat(breaker.stateName()).isEqualTo("closed");
        assertThat(breaker.tryAcquirePermission()).isTrue();
    }
}
