package com.kraft.lotto.feature.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("관리자 로그인 잠금 서비스")
class AdminLoginLockoutServiceTest {

    private static final String USER = "admin";
    private static final String IP   = "127.0.0.1";

    private AdminLoginLockoutService serviceWith(Clock clock) {
        return new AdminLoginLockoutService(
                new SimpleMeterRegistry(), clock, 3, 900L, 900L, 1000L);
    }

    private static Clock fixedAt(long epochSecond) {
        return Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("실패 횟수가 최대 시도 횟수 미만이면 잠기지 않는다")
    void notLockedBelowMaxAttempts() {
        var svc = serviceWith(fixedAt(1000));
        svc.recordFailure(USER, IP);
        svc.recordFailure(USER, IP);
        assertThat(svc.isLocked(USER, IP)).isFalse();
    }

    @Test
    @DisplayName("최대 시도 횟수에 도달하면 잠긴다")
    void lockedAtMaxAttempts() {
        var svc = serviceWith(fixedAt(1000));
        for (int i = 0; i < 3; i++) {
            svc.recordFailure(USER, IP);
        }
        assertThat(svc.isLocked(USER, IP)).isTrue();
    }

    @Test
    @DisplayName("잠금 만료는 최초 실패 시각이 아니라 잠금 시각을 기준으로 계산한다")
    void lockExpiryCountsFromLockedAtNotFirstAt() {
        Instant start = Instant.ofEpochSecond(0);
        Instant lockTrigger = Instant.ofEpochSecond(850);

        var svc = new AdminLoginLockoutService(
                new SimpleMeterRegistry(),
                Clock.fixed(start, ZoneOffset.UTC),
                3, 900L, 900L, 1000L);
        assertThat(svc.isLocked(USER, IP)).isFalse();

        var svcAtLock = new AdminLoginLockoutService(
                new SimpleMeterRegistry(),
                Clock.fixed(lockTrigger, ZoneOffset.UTC),
                3, 900L, 900L, 1000L);
        svcAtLock.recordFailure(USER, IP);
        svcAtLock.recordFailure(USER, IP);
        svcAtLock.recordFailure(USER, IP);
        assertThat(svcAtLock.isLocked(USER, IP)).isTrue();
    }

    @Test
    @DisplayName("잠금 시각 기준으로 잠금 시간이 지나면 해제된다")
    void unlockAfterLockoutExpiry() {
        var svc = serviceWith(fixedAt(0));
        svc.recordFailure(USER, IP);
        svc.recordFailure(USER, IP);
        svc.recordFailure(USER, IP);
        assertThat(svc.isLocked(USER, IP)).isTrue();

        var svcAfter = serviceWith(fixedAt(901));
        svcAfter.recordFailure(USER, IP);
        assertThat(svcAfter.isLocked(USER, IP)).isFalse();
    }

    @Test
    @DisplayName("성공 기록은 잠금 상태를 해제한다")
    void recordSuccessClearsLock() {
        var svc = serviceWith(fixedAt(1000));
        for (int i = 0; i < 3; i++) {
            svc.recordFailure(USER, IP);
        }
        assertThat(svc.isLocked(USER, IP)).isTrue();
        svc.recordSuccess(USER, IP);
        assertThat(svc.isLocked(USER, IP)).isFalse();
    }

    @Test
    @DisplayName("윈도우 시간이 지나면 실패 카운트가 초기화된다")
    void windowExpiryResetsCount() {
        var svcEarly = serviceWith(fixedAt(0));
        svcEarly.recordFailure(USER, IP);
        svcEarly.recordFailure(USER, IP);

        var svcLate = serviceWith(fixedAt(901));
        svcLate.recordFailure(USER, IP);
        assertThat(svcLate.isLocked(USER, IP)).isFalse();
    }

    @Test
    @DisplayName("다른 아이피의 실패는 독립적으로 계산된다")
    void differentIpsAreIndependent() {
        var svc = serviceWith(fixedAt(1000));
        for (int i = 0; i < 3; i++) {
            svc.recordFailure(USER, "10.0.0.1");
        }
        assertThat(svc.isLocked(USER, "10.0.0.1")).isTrue();
        assertThat(svc.isLocked(USER, "10.0.0.2")).isFalse();
    }
}
