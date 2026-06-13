package com.kraft.admin;

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

    public AdminLockoutFilter(AdminLoginAttemptService lockout) {
        this.lockout = lockout;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(req.getMethod())
                && "/admin/login".equals(req.getServletPath())) {
            String username = req.getParameter("username");
            String ip = req.getRemoteAddr();
            if (username != null && lockout.isLockedOut(username, ip)) {
                res.sendRedirect("/admin/login?locked");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
