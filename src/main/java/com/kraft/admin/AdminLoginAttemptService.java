package com.kraft.admin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * Tracks failed login attempts per username+IP.
 * Locks out after MAX_ATTEMPTS within the LOCKOUT_WINDOW.
 */
@Service
public class AdminLoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .expireAfterWrite(LOCKOUT_WINDOW)
            .maximumSize(5_000)
            .build();

    public void recordFailure(String username, String ip) {
        String key = key(username, ip);
        attempts.get(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void resetAttempts(String username, String ip) {
        attempts.invalidate(key(username, ip));
    }

    public boolean isLockedOut(String username, String ip) {
        AtomicInteger count = attempts.getIfPresent(key(username, ip));
        return count != null && count.get() >= MAX_ATTEMPTS;
    }

    private String key(String username, String ip) {
        return username + ":" + ip;
    }
}
