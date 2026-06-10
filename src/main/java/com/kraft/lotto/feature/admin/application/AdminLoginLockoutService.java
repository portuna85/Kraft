package com.kraft.lotto.feature.admin.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminLoginLockoutService {

    private static final String METRIC_NAME = "kraft.admin.login";

    private record AttemptWindow(AtomicInteger count, Instant firstAt, Instant lockedAt) {}

    private final Cache<String, AttemptWindow> windows;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final int maxAttempts;
    private final long windowSeconds;
    private final long lockoutSeconds;

    @Autowired
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AdminLoginLockoutService(
            MeterRegistry meterRegistry,
            Clock clock,
            @Value("${KRAFT_ADMIN_LOGIN_MAX_ATTEMPTS:5}") int maxAttempts,
            @Value("${KRAFT_ADMIN_LOGIN_WINDOW_SECONDS:900}") long windowSeconds,
            @Value("${KRAFT_ADMIN_LOGIN_LOCKOUT_SECONDS:900}") long lockoutSeconds,
            @Value("${KRAFT_ADMIN_LOGIN_MAX_WINDOW_SIZE:10000}") long maxWindowSize) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.lockoutSeconds = lockoutSeconds;
        this.windows = Caffeine.newBuilder()
                .maximumSize(maxWindowSize)
                .expireAfterAccess(Duration.ofSeconds(windowSeconds * 2 + lockoutSeconds))
                .build();
    }

    AdminLoginLockoutService() {
        this(new SimpleMeterRegistry(), Clock.systemDefaultZone(), 5, 900L, 900L, 10_000L);
    }

    public boolean isLocked(String username, String ip) {
        String key = key(username, ip);
        AttemptWindow w = windows.getIfPresent(key);
        if (w == null) {
            return false;
        }
        Instant now = clock.instant();
        if (w.lockedAt() != null) {
            long elapsedSinceLock = now.getEpochSecond() - w.lockedAt().getEpochSecond();
            if (elapsedSinceLock > lockoutSeconds) {
                windows.invalidate(key);
                return false;
            }
            return true;
        }
        long elapsedSinceFirst = now.getEpochSecond() - w.firstAt().getEpochSecond();
        if (elapsedSinceFirst > windowSeconds) {
            windows.invalidate(key);
            return false;
        }
        return false;
    }

    public void recordFailure(String username, String ip) {
        meterRegistry.counter(METRIC_NAME, "result", "failure").increment();
        String key = key(username, ip);
        Instant now = clock.instant();
        windows.asMap().compute(key, (k, existing) -> {
            if (existing == null) {
                return new AttemptWindow(new AtomicInteger(1), now, null);
            }
            long elapsed = now.getEpochSecond() - existing.firstAt().getEpochSecond();
            if (elapsed > windowSeconds) {
                return new AttemptWindow(new AtomicInteger(1), now, null);
            }
            int newCount = existing.count().incrementAndGet();
            Instant lockedAt = (existing.lockedAt() == null && newCount >= maxAttempts) ? now : existing.lockedAt();
            return new AttemptWindow(existing.count(), existing.firstAt(), lockedAt);
        });
        if (isLocked(username, ip)) {
            meterRegistry.counter(METRIC_NAME, "result", "locked").increment();
        }
    }

    public void recordSuccess(String username, String ip) {
        meterRegistry.counter(METRIC_NAME, "result", "success").increment();
        windows.invalidate(key(username, ip));
    }

    private static String key(String username, String ip) {
        return (username == null ? "" : username) + "|" + (ip == null ? "" : ip);
    }
}
