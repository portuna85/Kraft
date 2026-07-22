package com.kraft.admin;

import com.kraft.common.web.ClientIpResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects login POST when the username+IP is locked out,
 * before Spring Security's UsernamePasswordAuthenticationFilter runs.
 */
public class AdminLockoutFilter extends OncePerRequestFilter {

    private final AdminLoginAttemptService lockout;
    private final ClientIpResolver ipResolver;
    private final Counter lockoutCounter;

    public AdminLockoutFilter(AdminLoginAttemptService lockout, ClientIpResolver ipResolver,
                              MeterRegistry meterRegistry) {
        this.lockout = lockout;
        this.ipResolver = ipResolver;
        this.lockoutCounter = Counter.builder("kraft_admin_lockout_total")
                .description("관리자 로그인 lockout으로 거부된 시도 횟수")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(req.getMethod())
                && "/admin/login".equals(req.getServletPath())) {
            String username = req.getParameter("username");
            String ip = ipResolver.resolve(req);
            if (username != null && lockout.isLockedOut(username, ip)) {
                lockoutCounter.increment();
                res.sendRedirect("/admin/login?locked");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
