package com.kraft.lotto.infra.config;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.admin.application.AdminLoginLockoutService;
import com.kraft.lotto.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

public class AdminAuthFailureHandler implements AuthenticationFailureHandler {

    private final AdminAuditLogService auditLogService;
    private final AdminLoginLockoutService lockoutService;

    public AdminAuthFailureHandler(AdminAuditLogService auditLogService,
                                   AdminLoginLockoutService lockoutService) {
        this.auditLogService = auditLogService;
        this.lockoutService = lockoutService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String ip = ClientIpResolver.resolve(request, List.of());
        String username = request.getParameter("username");
        lockoutService.recordFailure(username, ip);
        auditLogService.recordFailure(username != null ? username : "unknown", "LOGIN_FAILED", null,
                ip, request.getHeader("User-Agent"), exception.getMessage());
        boolean locked = lockoutService.isLocked(username, ip);
        String redirect = locked ? "/admin/login?locked" : "/admin/login?error";
        response.sendRedirect(request.getContextPath() + redirect);
    }
}
