package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.web.dto.CollectStatusResponse;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class CollectionRunState {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> currentOperation = new AtomicReference<>(null);
    private final AtomicReference<Instant> startedAt = new AtomicReference<>(null);

    <T> T runExclusive(String operation, Supplier<T> action, Supplier<T> overlapFallback) {
        if (!running.compareAndSet(false, true)) {
            return overlapFallback.get();
        }
        currentOperation.set(operation);
        startedAt.set(Instant.now());
        try {
            return action.get();
        } finally {
            running.set(false);
            currentOperation.set(null);
            startedAt.set(null);
        }
    }

    CollectStatusResponse status() {
        if (!running.get()) {
            return CollectStatusResponse.idle();
        }
        return CollectStatusResponse.active(currentOperation.get(), startedAt.get());
    }
}
