package com.kraft.lotto.infra.config;

import com.kraft.lotto.feature.admin.application.AdminLoginLockoutService;
import com.kraft.lotto.support.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminLoginLockoutFilter extends OncePerRequestFilter {

    private final AdminLoginLockoutService lockoutService;

    public AdminLoginLockoutFilter(AdminLoginLockoutService lockoutService) {
        this.lockoutService = lockoutService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/admin/login".equals(request.getServletPath())) {
            String ip = ClientIpResolver.resolve(request, List.of());
            String username = request.getParameter("username");
            if (lockoutService.isLocked(username, ip)) {
                response.sendRedirect(request.getContextPath() + "/admin/login?locked");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
