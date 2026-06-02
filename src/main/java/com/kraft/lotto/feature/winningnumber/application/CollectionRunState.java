package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.web.dto.CollectStatusResponse;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class CollectionRunState {

    private record ActiveState(String operation, Instant startedAt) {}

    // null == idle, non-null == running
    private final AtomicReference<ActiveState> activeState = new AtomicReference<>(null);

    <T> T runExclusive(String operation, Supplier<T> action, Supplier<T> overlapFallback) {
        ActiveState newState = new ActiveState(operation, Instant.now());
        if (!activeState.compareAndSet(null, newState)) {
            return overlapFallback.get();
        }
        try {
            return action.get();
        } finally {
            activeState.set(null);
        }
    }

    CollectStatusResponse status() {
        ActiveState state = activeState.get();
        return state == null
                ? CollectStatusResponse.idle()
                : CollectStatusResponse.active(state.operation(), state.startedAt());
    }
}
