package com.kraft.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final String MDC_CLIENT_IP = "clientIp";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    private final ClientIpResolver clientIpResolver;

    public RequestIdFilter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER_NAME);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        long startedAt = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        MDC.put(MDC_CLIENT_IP, clientIpResolver.resolve(request));
        response.setHeader(HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            logRequest(request, response, elapsedMs);
            MDC.remove(MDC_KEY);
            MDC.remove(MDC_CLIENT_IP);
        }
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long elapsedMs) {
        int status = response.getStatus();
        String method = request.getMethod();
        String path = buildPath(request);
        String remote = request.getRemoteAddr();

        if (status >= 500) {
            log.error("HTTP {} {} -> status={} durationMs={} remote={}",
                    method, path, status, elapsedMs, remote);
        } else if (status >= 400) {
            log.warn("HTTP {} {} -> status={} durationMs={} remote={}",
                    method, path, status, elapsedMs, remote);
        } else {
            log.info("HTTP {} {} -> status={} durationMs={} remote={}",
                    method, path, status, elapsedMs, remote);
        }
    }

    private String buildPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return (query != null && !query.isBlank()) ? uri + "?" + query : uri;
    }
}
