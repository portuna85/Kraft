package com.kraft.lotto.feature.winningnumber.application;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

final class ApiRetrySupport {

    private static final long NO_DEADLINE = Long.MAX_VALUE;

    private final int retryBackoffMs;
    private final int requestTimeoutMs;
    private final LongSupplier nanoTime;
    private final DoubleSupplier jitterSource;
    private final Sleeper sleeper;

    ApiRetrySupport(int retryBackoffMs, int requestTimeoutMs) {
        this(
                retryBackoffMs,
                requestTimeoutMs,
                System::nanoTime,
                () -> ThreadLocalRandom.current().nextDouble(),
                Thread::sleep
        );
    }

    ApiRetrySupport(int retryBackoffMs,
                    int requestTimeoutMs,
                    LongSupplier nanoTime,
                    DoubleSupplier jitterSource,
                    Sleeper sleeper) {
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
        this.requestTimeoutMs = Math.max(0, requestTimeoutMs);
        this.nanoTime = nanoTime;
        this.jitterSource = jitterSource;
        this.sleeper = sleeper;
    }

    long deadlineFrom(long startedAtNanos) {
        if (requestTimeoutMs <= 0) {
            return NO_DEADLINE;
        }
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(requestTimeoutMs);
        long deadline = startedAtNanos + timeoutNanos;
        return deadline < startedAtNanos ? NO_DEADLINE : deadline;
    }

    long nowNanos() {
        return nanoTime.getAsLong();
    }

    void throwIfExpired(long deadlineNanos, String message) {
        if (deadlineNanos != NO_DEADLINE && nanoTime.getAsLong() >= deadlineNanos) {
            throw new ApiRequestTimeoutException(message);
        }
    }

    void sleepBeforeRetry(long deadlineNanos, String timeoutMessage, String interruptedMessage) {
        if (retryBackoffMs <= 0) {
            return;
        }
        throwIfExpired(deadlineNanos, timeoutMessage);
        long delayMs = jitteredBackoffMs(retryBackoffMs, jitterSource.getAsDouble());
        if (deadlineNanos != NO_DEADLINE) {
            long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - nanoTime.getAsLong());
            if (remainingMs <= 0) {
                throw new ApiRequestTimeoutException(timeoutMessage);
            }
            delayMs = Math.min(delayMs, remainingMs);
        }
        try {
            sleeper.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LottoApiClientException(interruptedMessage, ie);
        }
    }

    static long jitteredBackoffMs(int baseBackoffMs, double jitterUnit) {
        if (baseBackoffMs <= 0) {
            return 0;
        }
        double bounded = Math.clamp(jitterUnit, 0.0d, 1.0d);
        double factor = 0.5d + bounded;
        return Math.max(1L, Math.round(baseBackoffMs * factor));
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}

class ApiRequestTimeoutException extends LottoApiClientException {

    ApiRequestTimeoutException(String message) {
        super(message, FailureReason.TIMEOUT);
    }
}
