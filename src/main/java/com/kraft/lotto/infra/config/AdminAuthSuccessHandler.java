package com.kraft.lotto.infra.config;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.admin.application.AdminLoginLockoutService;
import com.kraft.lotto.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

public class AdminAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final AdminAuditLogService auditLogService;
    private final AdminLoginLockoutService lockoutService;

    public AdminAuthSuccessHandler(AdminAuditLogService auditLogService,
                                   AdminLoginLockoutService lockoutService) {
        this.auditLogService = auditLogService;
        this.lockoutService = lockoutService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String actor = authentication.getName();
        String ip = ClientIpResolver.resolve(request, List.of());
        lockoutService.recordSuccess(actor, ip);
        auditLogService.recordSuccess(actor, "LOGIN_SUCCESS", null, ip,
                request.getHeader("User-Agent"));
        response.sendRedirect(request.getContextPath() + "/admin/ops");
    }
}
