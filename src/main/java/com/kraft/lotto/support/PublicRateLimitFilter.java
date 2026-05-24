package com.kraft.lotto.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kraft.lotto.infra.config.KraftSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private final KraftSecurityProperties securityProperties;
    private final Cache<String, FixedWindowCounter> counters;

    @Autowired
    public PublicRateLimitFilter(ObjectProvider<KraftSecurityProperties> securityPropertiesProvider) {
        this(securityPropertiesProvider.getIfAvailable(KraftSecurityProperties::new));
    }

    PublicRateLimitFilter(KraftSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        long windowSeconds = Math.max(1L, securityProperties.getRateLimit().getWindowSeconds());
        this.counters = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(windowSeconds * 2L))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !("/".equals(path) || path.startsWith("/fragments/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = System.nanoTime();
        long windowNanos = TimeUnit.SECONDS.toNanos(securityProperties.getRateLimit().getWindowSeconds());
        int maxRequests = securityProperties.getRateLimit().getMaxRequests();

        String clientIp = ClientIpResolver.resolve(request, securityProperties.getTrustedProxies());
        String counterKey = clientIp + "|" + request.getRequestURI();
        FixedWindowCounter counter = counters.get(counterKey, key -> new FixedWindowCounter(now));

        FixedWindowCounter.Result result = counter.tryAcquire(now, maxRequests, windowNanos);
        response.setHeader("X-RateLimit-Limit", Integer.toString(maxRequests));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(result.remainingRequests()));
        response.setHeader("X-RateLimit-Reset", Long.toString(result.resetAfterSeconds()));
        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(result.retryAfterSeconds()));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Too many requests. Please retry later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    static final class FixedWindowCounter {

        private long windowStartedAtNanos;
        private int count;

        FixedWindowCounter(long nowNanos) {
            this.windowStartedAtNanos = nowNanos;
        }

        synchronized Result tryAcquire(long nowNanos, int maxRequests, long windowNanos) {
            if (nowNanos - windowStartedAtNanos >= windowNanos) {
                windowStartedAtNanos = nowNanos;
                count = 0;
            }
            count++;
            long retryAfterNanos = Math.max(0L, windowNanos - (nowNanos - windowStartedAtNanos));
            long retryAfterSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(retryAfterNanos));
            if (count <= maxRequests) {
                int remaining = Math.max(0, maxRequests - count);
                return new Result(true, 0L, remaining, retryAfterSeconds);
            }
            return new Result(false, retryAfterSeconds, 0, retryAfterSeconds);
        }

        record Result(boolean allowed, long retryAfterSeconds, int remainingRequests, long resetAfterSeconds) {
        }
    }
}
