package com.kraft.lotto.support;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    public static final String HEADER_NAME = "X-Request-Id";
    static final String MDC_REQUEST_ID = "requestId";
    static final String MDC_METHOD = "method";
    static final String MDC_PATH = "path";
    private static final int MAX_REQUEST_ID_LENGTH = 128;

    private final ObjectProvider<Tracer> tracerProvider;

    RequestIdFilter() {
        this.tracerProvider = null;
    }

    @Autowired
    public RequestIdFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER_NAME));
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_METHOD, LogSanitizer.sanitizeLogValue(request.getMethod()));
        MDC.put(MDC_PATH, LogSanitizer.maskSensitivePath(request.getRequestURI()));
        propagateRequestIdToBaggage(requestId);
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("completed status={} duration={}ms", response.getStatus(), (System.nanoTime() - started) / 1_000_000);
            MDC.remove(MDC_PATH);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private void propagateRequestIdToBaggage(String requestId) {
        if (tracerProvider == null) {
            return;
        }
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null || tracer.currentSpan() == null) {
            return;
        }
        tracer.currentSpan().tag("requestId", requestId);
    }

    private static String resolveRequestId(String headerValue) {
        String sanitized = LogSanitizer.sanitizeLogValue(headerValue)
                .replaceAll("[^A-Za-z0-9._:-]", "");
        if (sanitized.isBlank()) {
            return UUID.randomUUID().toString();
        }
        if (sanitized.length() > MAX_REQUEST_ID_LENGTH) {
            return sanitized.substring(0, MAX_REQUEST_ID_LENGTH);
        }
        return sanitized;
    }
}
