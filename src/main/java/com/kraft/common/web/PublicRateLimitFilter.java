package com.kraft.common.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kraft.common.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sliding-window rate limiter for public API endpoints.
 * B-4: trusted-proxy CIDR(172.28.0.0/16) 내부 IP는 우회 처리.
 */
@Component
@Order(10)
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ClientIpResolver clientIpResolver;
    private final Cache<String, AtomicInteger> counters;

    public PublicRateLimitFilter(SecurityProperties securityProperties,
                                 ClientIpResolver clientIpResolver) {
        this.securityProperties = securityProperties;
        this.clientIpResolver = clientIpResolver;
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(securityProperties.rateLimitMaxKeys())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String clientIp = clientIpResolver.resolve(request);

        // B-4: trusted-proxy CIDR 내부 트래픽(SSR 서버 등)은 rate-limit 우회
        if (clientIpResolver.isTrustedProxy(clientIp)) {
            chain.doFilter(request, response);
            return;
        }

        AtomicInteger counter = counters.get(clientIp, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        int limit = securityProperties.rateLimitPerMinute();

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - current)));

        if (current > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":429,"error":"Too Many Requests","code":"RATE_LIMIT_EXCEEDED",\
                    "message":"요청 횟수가 너무 많습니다. 잠시 후 다시 시도하세요."}""");
            return;
        }

        chain.doFilter(request, response);
    }
}
