package com.kraft.lotto.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kraft.lotto.infra.config.KraftSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final String API_V1_PREFIX = "/api/v1/";

    private static final java.util.List<String> RATE_LIMITED_PREFIXES = java.util.List.of(
            API_V1_PREFIX,
            "/latest", "/rounds", "/stats", "/analysis",
            "/companion", "/news", "/recommend", "/fragments/"
    );

    private final KraftSecurityProperties securityProperties;
    private final Cache<String, SlidingWindowCounter> counters;
    private final LongSupplier nanoTimeSupplier;
    private final ObjectMapper objectMapper;

    @Autowired
    public PublicRateLimitFilter(ObjectProvider<KraftSecurityProperties> securityPropertiesProvider,
                                 ObjectProvider<ObjectMapper> objectMapperProvider) {
        this(securityPropertiesProvider.getIfAvailable(KraftSecurityProperties::new),
             objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    PublicRateLimitFilter(KraftSecurityProperties securityProperties, ObjectMapper objectMapper) {
        this(securityProperties, objectMapper, System::nanoTime);
    }

    PublicRateLimitFilter(KraftSecurityProperties securityProperties, ObjectMapper objectMapper,
                          LongSupplier nanoTimeSupplier) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
        this.nanoTimeSupplier = nanoTimeSupplier;
        long windowSeconds = Math.max(1L, securityProperties.getRateLimit().getWindowSeconds());
        long maxKeys = Math.max(1L, securityProperties.getRateLimit().getMaxKeys());
        this.counters = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(windowSeconds * 2L))
                .maximumSize(maxKeys)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        // API 경로는 GET/POST 모두 제한 (POST /recommend, POST /analysis 가 고비용)
        if (path.startsWith(API_V1_PREFIX)) {
            return false;
        }
        // 웹 경로는 GET만 제한
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if ("/".equals(path)) {
            return false;
        }
        for (String prefix : RATE_LIMITED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = nanoTimeSupplier.getAsLong();
        long windowNanos = TimeUnit.SECONDS.toNanos(securityProperties.getRateLimit().getWindowSeconds());
        int maxRequests = securityProperties.getRateLimit().getMaxRequests();

        String clientIp = ClientIpResolver.resolve(request, securityProperties.getTrustedProxies());
        String counterKey = clientIp;
        SlidingWindowCounter counter = counters.get(counterKey, key -> new SlidingWindowCounter());

        SlidingWindowCounter.Result result = counter.tryAcquire(now, maxRequests, windowNanos);
        response.setHeader("X-RateLimit-Limit", Integer.toString(maxRequests));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(result.remainingRequests()));
        response.setHeader("X-RateLimit-Reset", Long.toString(result.resetAfterSeconds()));
        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(result.retryAfterSeconds()));
            if (request.getRequestURI().startsWith(API_V1_PREFIX)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        objectMapper.writeValueAsString(
                                ApiResponse.failure(ErrorCode.RATE_LIMITED)));
            } else {
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Too many requests. Please retry later.");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    static final class SlidingWindowCounter {

        private final Deque<Long> requestTimes = new ArrayDeque<>();

        synchronized Result tryAcquire(long nowNanos, int maxRequests, long windowNanos) {
            evictExpired(nowNanos, windowNanos);
            if (requestTimes.size() < maxRequests) {
                requestTimes.addLast(nowNanos);
                long resetAfterSeconds = requestTimes.isEmpty()
                        ? 1L
                        : secondsUntilExpiry(requestTimes.peekFirst(), nowNanos, windowNanos);
                return new Result(true, 0L, Math.max(0, maxRequests - requestTimes.size()), resetAfterSeconds);
            }
            long retryAfterSeconds = secondsUntilExpiry(requestTimes.peekFirst(), nowNanos, windowNanos);
            return new Result(false, retryAfterSeconds, 0, retryAfterSeconds);
        }

        private void evictExpired(long nowNanos, long windowNanos) {
            while (!requestTimes.isEmpty() && nowNanos - requestTimes.peekFirst() >= windowNanos) {
                requestTimes.removeFirst();
            }
        }

        private static long secondsUntilExpiry(long timestampNanos, long nowNanos, long windowNanos) {
            long remainingNanos = Math.max(0L, windowNanos - (nowNanos - timestampNanos));
            long seconds = TimeUnit.NANOSECONDS.toSeconds(remainingNanos);
            return Math.max(1L, seconds == 0L && remainingNanos > 0L ? 1L : seconds);
        }

        record Result(boolean allowed, long retryAfterSeconds, int remainingRequests, long resetAfterSeconds) {
        }
    }

    long estimatedCounterSize() {
        return counters.estimatedSize();
    }

    void cleanUpCounters() {
        counters.cleanUp();
    }
}
