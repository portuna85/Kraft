package com.kraft.lotto.infra.config;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

public class AdminAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final List<String> allowedEmails;
    private final AdminAuditLogService auditLogService;

    public AdminAuthSuccessHandler(List<String> allowedEmails, AdminAuditLogService auditLogService) {
        this.allowedEmails = List.copyOf(allowedEmails);
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String email = extractEmail(authentication);
        String ip = ClientIpResolver.resolve(request, List.of());

        if (email == null || !allowedEmails.contains(email)) {
            request.getSession().invalidate();
            if (auditLogService != null) {
                auditLogService.recordFailure(
                        email != null ? email : "unknown",
                        "LOGIN_DENIED", null, ip,
                        request.getHeader("User-Agent"),
                        "email not in allowed list");
            }
            response.sendRedirect(request.getContextPath() + "/admin/login?error=unauthorized");
            return;
        }

        if (auditLogService != null) {
            auditLogService.recordSuccess(email, "LOGIN_SUCCESS", null, ip,
                    request.getHeader("User-Agent"));
        }
        response.sendRedirect(request.getContextPath() + "/admin/ops");
    }

    private static String extractEmail(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauthUser) {
            Object emailAttr = oauthUser.getAttributes().get("email");
            return emailAttr != null ? emailAttr.toString() : null;
        }
        return null;
    }
}
