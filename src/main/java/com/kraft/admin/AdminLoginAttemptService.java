package com.kraft.admin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * Tracks failed login attempts per (username+IP) AND per username alone.
 * Locks out if either counter reaches its threshold within LOCKOUT_WINDOW.
 * IP 1차 방어가 가장 흔한 단일 IP brute-force를 빠르게 차단하고, 계정 단위
 * 임계값은 IP 분산 공격을 잡아내는 2차 방어다. 계정 임계값을 IP 임계값보다
 * 충분히 높게 잡아, 사용자명을 아는 익명 공격자가 소수 시도만으로 정상
 * 관리자의 계정을 잠가버리는 DoS를 어렵게 만든다.
 * NOTE: Caffeine 기반 — 단일 인스턴스 전용. 수평 확장 시 Redis 전환 필요.
 */
@Service
public class AdminLoginAttemptService {

    // admin_audit_log.admin_user 컬럼 길이(VARCHAR(100))와 동일해야 한다.
    static final int MAX_USERNAME_LENGTH = 100;

    private static final int MAX_ATTEMPTS_PER_IP = 5;
    private static final int MAX_ATTEMPTS_PER_ACCOUNT = 30;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .expireAfterWrite(LOCKOUT_WINDOW)
            .maximumSize(5_000)
            .build();

    private final Cache<String, AtomicInteger> accountAttempts = Caffeine.newBuilder()
            .expireAfterWrite(LOCKOUT_WINDOW)
            .maximumSize(1_000)
            .build();

    public void recordFailure(String username, String ip) {
        String normalized = normalizeUsername(username);
        attempts.get(key(normalized, ip), k -> new AtomicInteger(0)).incrementAndGet();
        accountAttempts.get(normalized, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void resetAttempts(String username, String ip) {
        String normalized = normalizeUsername(username);
        attempts.invalidate(key(normalized, ip));
        accountAttempts.invalidate(normalized);
    }

    public boolean isLockedOut(String username, String ip) {
        String normalized = normalizeUsername(username);
        AtomicInteger byIp = attempts.getIfPresent(key(normalized, ip));
        AtomicInteger byAccount = accountAttempts.getIfPresent(normalized);
        return (byIp != null && byIp.get() >= MAX_ATTEMPTS_PER_IP)
                || (byAccount != null && byAccount.get() >= MAX_ATTEMPTS_PER_ACCOUNT);
    }

    /**
     * 실패 로그인의 username은 외부 입력이므로 admin_audit_log.admin_user(VARCHAR(100)) 한도에 맞춰
     * trim·절단해 기록(감사 로그)과 잠금 캐시가 항상 같은 키를 보게 한다. 절단 없이는 100자 초과
     * username이 감사 로그 INSERT를 깨뜨려 로그인 실패 응답 자체가 500이 된다.
     */
    static String normalizeUsername(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= MAX_USERNAME_LENGTH) {
            return trimmed;
        }
        int cut = MAX_USERNAME_LENGTH;
        if (Character.isHighSurrogate(trimmed.charAt(cut - 1))) {
            cut--;
        }
        return trimmed.substring(0, cut);
    }

    private String key(String username, String ip) {
        return username + ":" + ip;
    }
}
