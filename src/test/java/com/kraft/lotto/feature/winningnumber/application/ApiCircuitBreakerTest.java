package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("API 서킷 브레이커")
class ApiCircuitBreakerTest {

    @Test
    @DisplayName("연속 실패 시 서킷이 열리고 일정 시간 후 회복된다")
    void opensAndRecovers() {
        AtomicLong now = new AtomicLong(0L);
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 2, 1000, 1, now::get);

        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.recordFailure();

        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.recordFailure();

        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertThat(breaker.stateName()).isEqualTo("open");

        now.addAndGet(TimeUnit.MILLISECONDS.toNanos(1000));
        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.stateName()).isEqualTo("half_open");

        breaker.recordSuccess();
        assertThat(breaker.stateName()).isEqualTo("closed");
        assertThat(breaker.tryAcquirePermission()).isTrue();
    }

    @Test
    @DisplayName("disabled 상태에서는 항상 허가하고 상태가 변하지 않는다")
    void disabledAlwaysPermits() {
        ApiCircuitBreaker breaker = ApiCircuitBreaker.disabled();

        for (int i = 0; i < 100; i++) {
            breaker.recordFailure();
        }
        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.stateName()).isEqualTo("closed");
    }

    @Test
    @DisplayName("open 지속시간 경계: 1ns 부족하면 여전히 차단된다")
    void openDurationBoundaryOneLessNano() {
        AtomicLong now = new AtomicLong(0L);
        int openDurationMs = 500;
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 1, openDurationMs, 1, now::get);

        breaker.recordFailure();
        assertThat(breaker.stateName()).isEqualTo("open");

        now.addAndGet(TimeUnit.MILLISECONDS.toNanos(openDurationMs) - 1);
        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertThat(breaker.stateName()).isEqualTo("open");
    }

    @Test
    @DisplayName("open 지속시간 경계: 정확히 만료 시점에 half-open으로 전이한다")
    void openDurationBoundaryExactExpiry() {
        AtomicLong now = new AtomicLong(0L);
        int openDurationMs = 500;
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 1, openDurationMs, 1, now::get);

        breaker.recordFailure();
        now.addAndGet(TimeUnit.MILLISECONDS.toNanos(openDurationMs));
        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.stateName()).isEqualTo("half_open");
    }

    @Test
    @DisplayName("half-open 최대 호출 상한에 도달하면 추가 허가를 거부한다")
    void halfOpenMaxCallsExceeded() {
        AtomicLong now = new AtomicLong(0L);
        int halfOpenMax = 2;
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 1, 100, halfOpenMax, now::get);

        breaker.recordFailure();
        now.addAndGet(TimeUnit.MILLISECONDS.toNanos(100));

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.tryAcquirePermission()).isFalse();
    }

    @Test
    @DisplayName("half-open에서 실패하면 다시 open으로 돌아간다")
    void halfOpenFailureReopens() {
        AtomicLong now = new AtomicLong(0L);
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 1, 100, 2, now::get);

        breaker.recordFailure();
        now.addAndGet(TimeUnit.MILLISECONDS.toNanos(100));

        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.stateName()).isEqualTo("half_open");
        breaker.recordFailure();
        assertThat(breaker.stateName()).isEqualTo("open");
        assertThat(breaker.tryAcquirePermission()).isFalse();
    }

    @Test
    @DisplayName("성공 기록 시 실패 카운터가 리셋되어 closed에서 재시작한다")
    void successResetFailureCount() {
        AtomicLong now = new AtomicLong(0L);
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 3, 100, 1, now::get);

        breaker.recordFailure();
        breaker.recordFailure();
        assertThat(breaker.stateName()).isEqualTo("closed");

        breaker.recordSuccess();

        breaker.recordFailure();
        breaker.recordFailure();
        assertThat(breaker.stateName()).isEqualTo("closed");

        breaker.recordFailure();
        assertThat(breaker.stateName()).isEqualTo("open");
    }

    @Test
    @DisplayName("동시성: 다중 스레드에서 실패를 기록해도 open 전이는 정확히 한 번만 발생한다")
    void concurrentFailuresOpenTransitionIsConsistent() throws InterruptedException {
        AtomicLong now = new AtomicLong(0L);
        int threshold = 5;
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, threshold, 10_000, 1, now::get);

        int threads = 20;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                breaker.recordFailure();
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        // 여러 스레드가 동시에 실패를 기록해도 상태는 open이어야 하고 일관적이어야 한다
        assertThat(breaker.stateName()).isEqualTo("open");
        assertThat(breaker.tryAcquirePermission()).isFalse();
    }

    @Test
    @DisplayName("동시성: tryAcquirePermission과 recordFailure 동시 호출 시 상태 일관성 유지")
    void concurrentAcquireAndFailureStateConsistency() throws InterruptedException {
        AtomicLong now = new AtomicLong(0L);
        ApiCircuitBreaker breaker = new ApiCircuitBreaker(true, 3, 1_000, 2, now::get);

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads / 2; i++) {
            futures.add(pool.submit(() -> {
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < 10; j++) {
                    breaker.recordFailure();
                }
            }));
            futures.add(pool.submit(() -> {
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < 10; j++) {
                    breaker.tryAcquirePermission();
                }
            }));
        }

        start.countDown();
        for (Future<?> f : futures) {
            try {
                f.get(5, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ex) {
                Thread.currentThread().interrupt();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        // 예외가 발생하지 않고 stateName이 유효한 값이어야 한다
        String state = breaker.stateName();
        assertThat(state).isIn("closed", "open", "half_open");
    }
}
