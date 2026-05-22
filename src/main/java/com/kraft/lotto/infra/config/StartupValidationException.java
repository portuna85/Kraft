package com.kraft.lotto.infra.config;

/**
 * Startup validation failures are user-actionable configuration errors, so keep output concise.
 */
public final class StartupValidationException extends IllegalStateException {

    public StartupValidationException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
