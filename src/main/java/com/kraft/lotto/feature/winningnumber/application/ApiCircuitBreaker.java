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

    boolean tryAcquirePermission() {
        if (!enabled) {
            return true;
        }
        StateTransition transition = null;
        synchronized (this) {
            long now = nanoTime.getAsLong();
            if (state == State.OPEN) {
                long elapsed = now - openedAtNanos;
                if (elapsed < openDurationNanos) {
                    return false;
                }
                transition = transitionState(State.HALF_OPEN);
                halfOpenCalls = 0;
            }

            if (state == State.HALF_OPEN) {
                if (halfOpenCalls >= halfOpenMaxCalls) {
                    return false;
                }
                halfOpenCalls++;
                notifyTransition(transition);
                return true;
            }
        }
        notifyTransition(transition);
        return true;
    }

    void recordSuccess() {
        if (!enabled) {
            return;
        }
        StateTransition transition;
        synchronized (this) {
            transition = transitionState(State.CLOSED);
            consecutiveFailures = 0;
            halfOpenCalls = 0;
        }
        notifyTransition(transition);
    }

    void recordFailure() {
        if (!enabled) {
            return;
        }
        StateTransition transition = null;
        synchronized (this) {
            if (state == State.HALF_OPEN) {
                transition = openTransition();
            } else if (state != State.OPEN) {
                consecutiveFailures++;
                if (consecutiveFailures >= failureThreshold) {
                    transition = openTransition();
                }
            }
        }
        notifyTransition(transition);
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

    private StateTransition openTransition() {
        StateTransition transition = transitionState(State.OPEN);
        openedAtNanos = nanoTime.getAsLong();
        consecutiveFailures = 0;
        halfOpenCalls = 0;
        return transition;
    }

    private StateTransition transitionState(State next) {
        if (state == next) {
            return null;
        }
        State prev = state;
        state = next;
        return new StateTransition(prev, next);
    }

    private void notifyTransition(StateTransition transition) {
        if (transition == null || stateTransitionListener == null) {
            return;
        }
        stateTransitionListener.onTransition(
                transition.previous().name().toLowerCase(),
                transition.next().name().toLowerCase()
        );
    }

    interface StateTransitionListener {
        void onTransition(String previousState, String nextState);
    }

    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private record StateTransition(State previous, State next) {
    }
}
