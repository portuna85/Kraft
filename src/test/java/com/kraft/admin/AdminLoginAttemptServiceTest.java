package com.kraft.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("관리자 로그인 시도 서비스 테스트")
class AdminLoginAttemptServiceTest {

    private AdminLoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new AdminLoginAttemptService();
    }

    @Test
    @DisplayName("로그인 시도가 없을 때 잠금 상태가 아닌지 확인")
    void isLockedOut_returnsFalse_whenNoAttempts() {
        assertThat(service.isLockedOut("admin", "1.2.3.4")).isFalse();
    }

    @Test
    @DisplayName("실패 횟수가 제한 미만일 때 잠금 상태가 아닌지 확인")
    void isLockedOut_returnsFalse_whenBelowLimit() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        assertThat(service.isLockedOut("admin", "1.2.3.4")).isFalse();
    }

    @Test
    @DisplayName("실패 횟수가 제한 이상일 때 잠금 상태인지 확인")
    void isLockedOut_returnsTrue_whenAtOrAboveLimit() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        assertThat(service.isLockedOut("admin", "1.2.3.4")).isTrue();
    }

    @Test
    @DisplayName("동일 주소에서의 5회 실패만으로는 다른 주소를 잠그지 않는다")
    void isLockedOut_singleIpFailures_doNotLockOtherIpsViaAccountThreshold() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        // same user, different IP — IP-level counter is per-IP(0), account-level counter(5) is still
        // below the higher account threshold → 익명 공격자가 단일 IP로 정상 관리자 계정을 DoS 못 함
        assertThat(service.isLockedOut("admin", "9.9.9.9")).isFalse();
        // different user, same IP — should not be locked (different account)
        assertThat(service.isLockedOut("other", "1.2.3.4")).isFalse();
    }

    @Test
    @DisplayName("여러 주소에 분산된 실패가 계정 임계값에 도달하면 모든 주소에서 잠긴다")
    void isLockedOut_distributedFailuresAcrossManyIps_triggersAccountLockout() {
        for (int i = 0; i < 30; i++) {
            service.recordFailure("admin", "10.0.0." + i);
        }
        assertThat(service.isLockedOut("admin", "9.9.9.9")).isTrue();
    }

    @Test
    @DisplayName("100자 초과 username은 절단된 키로 기록·판정되어 원본과 동일하게 잠긴다")
    void isLockedOut_overlongUsername_usesNormalizedKeyConsistently() {
        String overlong = "a".repeat(150);
        for (int i = 0; i < 5; i++) {
            service.recordFailure(overlong, "1.2.3.4");
        }
        // 절단 결과가 같은 다른 원본(prefix 100자 동일)도 같은 키로 잠금 판정된다
        assertThat(service.isLockedOut(overlong, "1.2.3.4")).isTrue();
        assertThat(service.isLockedOut("a".repeat(120), "1.2.3.4")).isTrue();
    }

    @Test
    @DisplayName("normalizeUsername은 trim 후 100자로 절단하고 서로게이트 페어를 자르지 않는다")
    void normalizeUsername_trimsAndTruncatesWithoutSplittingSurrogatePairs() {
        assertThat(AdminLoginAttemptService.normalizeUsername(null)).isNull();
        assertThat(AdminLoginAttemptService.normalizeUsername("  admin  ")).isEqualTo("admin");
        assertThat(AdminLoginAttemptService.normalizeUsername("a".repeat(150))).hasSize(100);

        // 99자 + 서로게이트 페어(😀=2 char) → 100번째 char가 high surrogate이므로 99자로 절단
        String surrogateBoundary = "a".repeat(99) + "😀".repeat(5);
        String normalized = AdminLoginAttemptService.normalizeUsername(surrogateBoundary);
        assertThat(normalized).hasSize(99);
    }

    @Test
    @DisplayName("로그인 시도 초기화 시 잠금이 해제되는지 확인")
    void resetAttempts_clearsLockout() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        assertThat(service.isLockedOut("admin", "1.2.3.4")).isTrue();

        service.resetAttempts("admin", "1.2.3.4");

        assertThat(service.isLockedOut("admin", "1.2.3.4")).isFalse();
    }
}
