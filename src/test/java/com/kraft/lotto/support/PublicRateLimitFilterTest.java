package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import com.kraft.lotto.support.PublicRateLimitFilter.FixedWindowCounter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("공개 경로 속도 제한 필터")
class PublicRateLimitFilterTest {

    @Test
    @DisplayName("공개 프래그먼트 경로에서 요청 한도를 초과하면 429를 반환한다")
    void returnsTooManyRequestsAfterThreshold() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setMaxRequests(2);
        properties.getRateLimit().setWindowSeconds(60);
        PublicRateLimitFilter filter = new PublicRateLimitFilter(properties);

        MockHttpServletResponse first = execute(filter, "/fragments/recommend", "198.51.100.11");
        MockHttpServletResponse second = execute(filter, "/fragments/recommend", "198.51.100.11");
        MockHttpServletResponse third = execute(filter, "/fragments/recommend", "198.51.100.11");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isNotBlank();
        assertThat(third.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(third.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(third.getHeader("X-RateLimit-Reset")).isNotBlank();
    }

    @Test
    @DisplayName("공개 경로가 아닌 경우 속도 제한을 적용하지 않는다")
    void doesNotRateLimitNonPublicPaths() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setMaxRequests(1);
        properties.getRateLimit().setWindowSeconds(60);
        PublicRateLimitFilter filter = new PublicRateLimitFilter(properties);

        MockHttpServletResponse first = execute(filter, "/css/app.css", "198.51.100.11");
        MockHttpServletResponse second = execute(filter, "/css/app.css", "198.51.100.11");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(first.getHeader("X-RateLimit-Limit")).isNull();
        assertThat(second.getHeader("X-RateLimit-Limit")).isNull();
    }

    @Test
    @DisplayName("카운터 캐시 키 수는 설정된 상한을 넘지 않는다")
    void boundsCounterCacheSize() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setMaxRequests(1);
        properties.getRateLimit().setWindowSeconds(60);
        properties.getRateLimit().setMaxKeys(32);
        PublicRateLimitFilter filter = new PublicRateLimitFilter(properties);

        for (int i = 0; i < 2_000; i++) {
            execute(filter, "/fragments/recommend", "198.51.100." + i);
        }

        filter.cleanUpCounters();
        assertThat(filter.estimatedCounterSize()).isLessThanOrEqualTo(32);
    }

    // --- FixedWindowCounter 단위 테스트 ---

    @Test
    @DisplayName("FixedWindowCounter: 윈도우 내 한도 초과 직후 remaining은 0이다")
    void fixedWindowRemainingIsZeroAfterExhaustion() {
        long startNanos = 0L;
        FixedWindowCounter counter = new FixedWindowCounter(startNanos);
        long windowNanos = TimeUnit.SECONDS.toNanos(60);
        int maxRequests = 3;

        FixedWindowCounter.Result r1 = counter.tryAcquire(startNanos, maxRequests, windowNanos);
        FixedWindowCounter.Result r2 = counter.tryAcquire(startNanos, maxRequests, windowNanos);
        FixedWindowCounter.Result r3 = counter.tryAcquire(startNanos, maxRequests, windowNanos);
        FixedWindowCounter.Result r4 = counter.tryAcquire(startNanos, maxRequests, windowNanos);

        assertThat(r1.allowed()).isTrue();
        assertThat(r2.allowed()).isTrue();
        assertThat(r3.allowed()).isTrue();
        assertThat(r3.remainingRequests()).isEqualTo(0);
        assertThat(r4.allowed()).isFalse();
        assertThat(r4.remainingRequests()).isEqualTo(0);
        assertThat(r4.retryAfterSeconds()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("FixedWindowCounter: 윈도우 롤오버 시 카운트가 리셋된다")
    void fixedWindowRolloverResetsCount() {
        long startNanos = 0L;
        long windowNanos = TimeUnit.SECONDS.toNanos(10);
        int maxRequests = 1;
        FixedWindowCounter counter = new FixedWindowCounter(startNanos);

        FixedWindowCounter.Result first = counter.tryAcquire(startNanos, maxRequests, windowNanos);
        FixedWindowCounter.Result blocked = counter.tryAcquire(startNanos, maxRequests, windowNanos);

        assertThat(first.allowed()).isTrue();
        assertThat(blocked.allowed()).isFalse();

        // 윈도우 경계 정확히 만료 후 첫 요청은 허가돼야 한다
        long afterWindow = startNanos + windowNanos;
        FixedWindowCounter.Result afterRollover = counter.tryAcquire(afterWindow, maxRequests, windowNanos);
        assertThat(afterRollover.allowed()).isTrue();
    }

    @Test
    @DisplayName("FixedWindowCounter: 동시성 — N 스레드 동시 요청 시 허가 수가 maxRequests를 초과하지 않는다")
    void fixedWindowConcurrentRequestsNeverExceedMax() throws InterruptedException {
        long startNanos = System.nanoTime();
        long windowNanos = TimeUnit.SECONDS.toNanos(60);
        int maxRequests = 10;
        FixedWindowCounter counter = new FixedWindowCounter(startNanos);

        int threads = 50;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return counter.tryAcquire(System.nanoTime(), maxRequests, windowNanos).allowed();
            }));
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        long allowedCount = futures.stream()
                .mapToLong(f -> {
                    try { return f.get() ? 1L : 0L; } catch (Exception e) { return 0L; }
                })
                .sum();

        assertThat(allowedCount).isLessThanOrEqualTo(maxRequests);
    }

    private static MockHttpServletResponse execute(PublicRateLimitFilter filter,
                                                   String path,
                                                   String remoteAddr) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
