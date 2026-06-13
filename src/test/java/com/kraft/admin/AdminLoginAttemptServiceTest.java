package com.kraft.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLoginAttemptServiceTest {

    private AdminLoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new AdminLoginAttemptService();
    }

    @Test
    void isLockedOut_returnsFalse_whenNoAttempts() {
        assertThat(service.isLockedOut("admin", "1.2.3.4")).isFalse();
    }

    @Test
    void isLockedOut_returnsFalse_whenBelowLimit() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        assertThat(service.isLockedOut("admin", "1.2.3.4")).isFalse();
    }

    @Test
    void isLockedOut_returnsTrue_whenAtOrAboveLimit() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        assertThat(service.isLockedOut("admin", "1.2.3.4")).isTrue();
    }

    @Test
    void isLockedOut_isIsolatedByUsernameAndIp() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        // different IP — should not be locked
        assertThat(service.isLockedOut("admin", "9.9.9.9")).isFalse();
        // different user — should not be locked
        assertThat(service.isLockedOut("other", "1.2.3.4")).isFalse();
    }

    @Test
    void resetAttempts_clearsLockout() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin", "1.2.3.4");
        }
        assertThat(service.isLockedOut("admin", "1.2.3.4")).isTrue();

        service.resetAttempts("admin", "1.2.3.4");

        assertThat(service.isLockedOut("admin", "1.2.3.4")).isFalse();
    }
}
