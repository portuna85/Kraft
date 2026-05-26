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
    private StateTransitionListener stateTransitionListener;

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
            transitionTo(State.HALF_OPEN);
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
        transitionTo(State.CLOSED);
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

    synchronized int stateCode() {
        return switch (state) {
            case CLOSED -> 0;
            case HALF_OPEN -> 1;
            case OPEN -> 2;
        };
    }

    synchronized boolean enabled() {
        return enabled;
    }

    synchronized void setStateTransitionListener(StateTransitionListener listener) {
        this.stateTransitionListener = listener;
    }

    private void open() {
        transitionTo(State.OPEN);
        openedAtNanos = nanoTime.getAsLong();
        consecutiveFailures = 0;
        halfOpenCalls = 0;
    }

    private void transitionTo(State next) {
        if (state == next) {
            return;
        }
        State prev = state;
        state = next;
        if (stateTransitionListener != null) {
            stateTransitionListener.onTransition(prev.name().toLowerCase(), next.name().toLowerCase());
        }
    }

    interface StateTransitionListener {
        void onTransition(String previousState, String nextState);
    }

    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
