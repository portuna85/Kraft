package com.kraft.admin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * Tracks failed login attempts per (username+IP) AND per username alone.
 * Locks out if either counter reaches MAX_ATTEMPTS within LOCKOUT_WINDOW,
 * preventing multi-IP distributed brute-force that could bypass per-IP limits.
 */
@Service
public class AdminLoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
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
        return (byIp != null && byIp.get() >= MAX_ATTEMPTS)
                || (byAccount != null && byAccount.get() >= MAX_ATTEMPTS);
    }

    private String key(String username, String ip) {
        return username + ":" + ip;
    }
}
