package com.kraft.common.web;

import com.kraft.common.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sliding-window rate limiter for public API endpoints.
 * trusted-proxy CIDR(172.28.0.0/16) 내부 IP는 우회 처리.
 */
@Component
@Order(10)
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PublicRateLimitFilter.class);
    private static final String RATE_LIMIT_EXCEEDED_BODY =
            """
            {"status":429,"error":"Too Many Requests","code":"RATE_LIMIT_EXCEEDED",\
            "message":"요청 횟수가 너무 많습니다. 잠시 후 다시 시도하세요."}""";

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
        return !path.startsWith("/api/") && !path.startsWith("/ops/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String clientIp = clientIpResolver.resolve(request);

        if (clientIpResolver.isTrustedProxy(clientIp)) {
            chain.doFilter(request, response);
            return;
        }

        int limit = securityProperties.rateLimitPerMinute();
        int current = counters.get(clientIp, k -> new AtomicInteger(0)).incrementAndGet();

        response.setIntHeader("X-RateLimit-Limit", limit);
        response.setIntHeader("X-RateLimit-Remaining", Math.max(0, limit - current));

        if (current > limit) {
            log.warn("Rate limit 초과: ip={} count={} limit={}", clientIp, current, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(RATE_LIMIT_EXCEEDED_BODY);
            return;
        }

        chain.doFilter(request, response);
    }
}
