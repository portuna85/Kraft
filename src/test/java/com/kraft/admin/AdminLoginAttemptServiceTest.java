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
    @DisplayName("동일 IP에서의 5회 실패만으로는 다른 IP를 잠그지 않는다 (계정 임계값은 IP 임계값보다 높음)")
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
    @DisplayName("여러 IP에 분산된 실패가 계정 임계값에 도달하면 모든 IP에서 잠긴다 (분산 brute-force 방어)")
    void isLockedOut_distributedFailuresAcrossManyIps_triggersAccountLockout() {
        for (int i = 0; i < 30; i++) {
            service.recordFailure("admin", "10.0.0." + i);
        }
        assertThat(service.isLockedOut("admin", "9.9.9.9")).isTrue();
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
