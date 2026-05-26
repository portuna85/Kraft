package com.kraft.lotto.feature.winningnumber.application;

import org.springframework.web.client.RestClientException;

final class ApiCallExecutor {

    private ApiCallExecutor() {
    }

    @FunctionalInterface
    interface AttemptAction<T> {
        T run(long deadlineNanos);
    }

    @FunctionalInterface
    interface RestClientFailureHandler {
        void handle(int attempt, int attempts, long deadlineNanos, RestClientException ex);
    }

    @FunctionalInterface
    interface ClientFailureHandler {
        void handle(int attempt, int attempts, long deadlineNanos, LottoApiClientException ex);
    }

    @FunctionalInterface
    interface CircuitOpenHandler {
        void onOpen();
    }

    static <T> T executeWithRetry(ApiRetrySupport retrySupport,
                                  int maxRetries,
                                  ApiCircuitBreaker circuitBreaker,
                                  String timeoutMessage,
                                  String circuitOpenMessage,
                                  AttemptAction<T> attemptAction,
                                  RestClientFailureHandler restFailureHandler,
                                  ClientFailureHandler clientFailureHandler,
                                  CircuitOpenHandler circuitOpenHandler) {
        long started = retrySupport.nowNanos();
        long deadline = retrySupport.deadlineFrom(started);
        int attempts = maxRetries + 1;
        int attempt = 0;

        while (true) {
            retrySupport.throwIfExpired(deadline, timeoutMessage);
            attempt++;
            if (!circuitBreaker.tryAcquirePermission()) {
                circuitOpenHandler.onOpen();
                throw new CircuitBreakerOpenException(circuitOpenMessage);
            }
            try {
                return attemptAction.run(deadline);
            } catch (RestClientException ex) {
                restFailureHandler.handle(attempt, attempts, deadline, ex);
            } catch (LottoApiClientException ex) {
                clientFailureHandler.handle(attempt, attempts, deadline, ex);
            }
        }
    }
}
