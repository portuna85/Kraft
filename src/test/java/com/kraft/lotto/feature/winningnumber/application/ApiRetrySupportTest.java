package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("API 재시도 지원")
class ApiRetrySupportTest {

    @Test
    @DisplayName("재시도 백오프는 50%에서 150% 사이의 지터를 사용한다")
    void jitteredBackoffUsesExpectedRange() {
        assertThat(ApiRetrySupport.jitteredBackoffMs(100, 0.0d)).isEqualTo(50L);
        assertThat(ApiRetrySupport.jitteredBackoffMs(100, 0.5d)).isEqualTo(100L);
        assertThat(ApiRetrySupport.jitteredBackoffMs(100, 1.0d)).isEqualTo(150L);
    }

    @Test
    @DisplayName("재시도 대기 시간은 남은 요청 마감 시간으로 제한된다")
    void sleepBeforeRetryCapsDelayToRemainingDeadline() {
        AtomicLong now = new AtomicLong(0L);
        List<Long> sleeps = new ArrayList<>();
        ApiRetrySupport support = new ApiRetrySupport(
                100,
                75,
                now::get,
                () -> 0.5d,
                delayMs -> {
                    sleeps.add(delayMs);
                    now.addAndGet(TimeUnit.MILLISECONDS.toNanos(delayMs));
                }
        );

        long deadline = support.deadlineFrom(now.get());

        support.sleepBeforeRetry(deadline, "timeout", "interrupted");

        assertThat(sleeps).containsExactly(75L);
        assertThatThrownBy(() -> support.throwIfExpired(deadline, "timeout"))
                .isInstanceOf(ApiRequestTimeoutException.class);
    }
}
