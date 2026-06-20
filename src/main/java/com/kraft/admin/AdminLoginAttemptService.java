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
        attempts.get(key(username, ip), k -> new AtomicInteger(0)).incrementAndGet();
        accountAttempts.get(username, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void resetAttempts(String username, String ip) {
        attempts.invalidate(key(username, ip));
        accountAttempts.invalidate(username);
    }

    public boolean isLockedOut(String username, String ip) {
        AtomicInteger byIp = attempts.getIfPresent(key(username, ip));
        AtomicInteger byAccount = accountAttempts.getIfPresent(username);
        return (byIp != null && byIp.get() >= MAX_ATTEMPTS_PER_IP)
                || (byAccount != null && byAccount.get() >= MAX_ATTEMPTS_PER_ACCOUNT);
    }

    private String key(String username, String ip) {
        return username + ":" + ip;
    }
}
