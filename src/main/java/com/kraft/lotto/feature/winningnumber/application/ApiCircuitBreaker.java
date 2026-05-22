package com.kraft.lotto.feature.winningnumber.application;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

final class ApiCircuitBreaker {

    private final boolean enabled;
    private final int failureThreshold;
    private final long openDurationNanos;
    private final int halfOpenMaxCalls;
    private final LongSupplier nanoTime;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private int halfOpenCalls = 0;
    private long openedAtNanos = 0L;

    ApiCircuitBreaker(boolean enabled,
                      int failureThreshold,
                      int openDurationMs,
                      int halfOpenMaxCalls) {
        this(enabled, failureThreshold, openDurationMs, halfOpenMaxCalls, System::nanoTime);
    }

    ApiCircuitBreaker(boolean enabled,
                      int failureThreshold,
                      int openDurationMs,
                      int halfOpenMaxCalls,
                      LongSupplier nanoTime) {
        this.enabled = enabled;
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDurationNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(1, openDurationMs));
        this.halfOpenMaxCalls = Math.max(1, halfOpenMaxCalls);
        this.nanoTime = nanoTime;
    }

    static ApiCircuitBreaker disabled() {
        return new ApiCircuitBreaker(false, 1, 1, 1);
    }

    synchronized boolean tryAcquirePermission() {
        if (!enabled) {
            return true;
        }
        long now = nanoTime.getAsLong();
        if (state == State.OPEN) {
            long elapsed = now - openedAtNanos;
            if (elapsed < openDurationNanos) {
                return false;
            }
            state = State.HALF_OPEN;
            halfOpenCalls = 0;
        }

        if (state == State.HALF_OPEN) {
            if (halfOpenCalls >= halfOpenMaxCalls) {
                return false;
            }
            halfOpenCalls++;
            return true;
        }

        return true;
    }

    synchronized void recordSuccess() {
        if (!enabled) {
            return;
        }
        state = State.CLOSED;
        consecutiveFailures = 0;
        halfOpenCalls = 0;
    }

    synchronized void recordFailure() {
        if (!enabled) {
            return;
        }

        if (state == State.HALF_OPEN) {
            open();
            return;
        }

        if (state == State.OPEN) {
            return;
        }

        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            open();
        }
    }

    synchronized String stateName() {
        return state.name().toLowerCase();
    }

    private void open() {
        state = State.OPEN;
        openedAtNanos = nanoTime.getAsLong();
        consecutiveFailures = 0;
        halfOpenCalls = 0;
    }

    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
