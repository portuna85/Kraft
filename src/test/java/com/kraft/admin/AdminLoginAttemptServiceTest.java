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
    @DisplayName("사용자 이름과 IP 주소별로 잠금 상태가 격리되는지 확인")
    void isLockedOut_isIsolatedByUsernameAndIp() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        // same user, different IP — account-level counter triggers lockout (distributed brute-force protection)
        assertThat(service.isLockedOut("admin", "9.9.9.9")).isTrue();
        // different user, same IP — should not be locked (different account)
        assertThat(service.isLockedOut("other", "1.2.3.4")).isFalse();
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
