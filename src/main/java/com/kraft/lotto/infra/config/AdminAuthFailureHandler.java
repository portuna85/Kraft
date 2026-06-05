package com.kraft.lotto.infra.config;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

public class AdminAuthFailureHandler implements AuthenticationFailureHandler {

    private final AdminAuditLogService auditLogService;

    public AdminAuthFailureHandler(AdminAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String ip = ClientIpResolver.resolve(request, List.of());
        if (auditLogService != null) {
            auditLogService.recordFailure("unknown", "LOGIN_FAILED", null,
                    ip, request.getHeader("User-Agent"), exception.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/login?error");
    }
}
