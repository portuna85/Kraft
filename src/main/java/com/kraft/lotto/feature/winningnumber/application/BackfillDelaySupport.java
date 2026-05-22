package com.kraft.lotto.feature.winningnumber.application;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

final class BackfillDelaySupport {

    private final long delayNanos;
    private final NanosPauser pauser;

    BackfillDelaySupport(long delayMs) {
        this(delayMs, LockSupport::parkNanos);
    }

    BackfillDelaySupport(long delayMs, NanosPauser pauser) {
        this.delayNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, delayMs));
        this.pauser = pauser;
    }

    boolean pauseIfPossible() {
        if (delayNanos <= 0L) {
            return true;
        }
        pauser.pause(delayNanos);
        return !Thread.currentThread().isInterrupted();
    }

    @FunctionalInterface
    interface NanosPauser {
        void pause(long nanos);
    }
}
