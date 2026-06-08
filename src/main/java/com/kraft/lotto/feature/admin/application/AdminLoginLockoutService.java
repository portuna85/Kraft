package com.kraft.lotto.feature.admin.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminLoginLockoutService {

    private static final String METRIC_NAME = "kraft.admin.login";

    private record AttemptWindow(AtomicInteger count, Instant firstAt) {}

    private final ConcurrentHashMap<String, AttemptWindow> windows = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Value("${KRAFT_ADMIN_LOGIN_MAX_ATTEMPTS:5}")
    private int maxAttempts;

    @Value("${KRAFT_ADMIN_LOGIN_WINDOW_SECONDS:900}")
    private long windowSeconds;

    @Value("${KRAFT_ADMIN_LOGIN_LOCKOUT_SECONDS:900}")
    private long lockoutSeconds;

    public AdminLoginLockoutService(MeterRegistry meterRegistry, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    AdminLoginLockoutService() {
        this(new SimpleMeterRegistry(), Clock.systemDefaultZone());
    }

    public boolean isLocked(String username, String ip) {
        String key = key(username, ip);
        AttemptWindow w = windows.get(key);
        if (w == null) {
            return false;
        }
        Instant now = clock.instant();
        long elapsedSeconds = now.getEpochSecond() - w.firstAt().getEpochSecond();
        if (elapsedSeconds > lockoutSeconds) {
            windows.remove(key);
            return false;
        }
        return w.count().get() >= maxAttempts;
    }

    public void recordFailure(String username, String ip) {
        meterRegistry.counter(METRIC_NAME, "result", "failure").increment();
        String key = key(username, ip);
        Instant now = clock.instant();
        windows.compute(key, (k, existing) -> {
            if (existing == null) {
                AttemptWindow w = new AttemptWindow(new AtomicInteger(1), now);
                return w;
            }
            long elapsed = now.getEpochSecond() - existing.firstAt().getEpochSecond();
            if (elapsed > windowSeconds) {
                return new AttemptWindow(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });
        if (isLocked(username, ip)) {
            meterRegistry.counter(METRIC_NAME, "result", "locked").increment();
        }
    }

    public void recordSuccess(String username, String ip) {
        meterRegistry.counter(METRIC_NAME, "result", "success").increment();
        windows.remove(key(username, ip));
    }

    private static String key(String username, String ip) {
        return (username == null ? "" : username) + "|" + (ip == null ? "" : ip);
    }
}
