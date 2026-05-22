package com.kraft.lotto.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    static final String MDC_REQUEST_ID = "requestId";
    static final String MDC_METHOD = "method";
    static final String MDC_PATH = "path";
    private static final int MAX_REQUEST_ID_LENGTH = 128;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER_NAME));
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_METHOD, LogSanitizer.sanitizeLogValue(request.getMethod()));
        MDC.put(MDC_PATH, LogSanitizer.maskSensitivePath(request.getRequestURI()));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_PATH);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_REQUEST_ID);
        }
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
