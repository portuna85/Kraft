package com.kraft.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(5)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/h2-console")) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        // S-4: API/actuator 응답에 최소 CSP 적용 (브라우저 직접 접근 시 렌더링 표면 최소화)
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        chain.doFilter(request, response);
    }
}
