package com.kraft.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class AdminLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final AdminLoginAttemptService lockout;
    private final AdminAuditLogService audit;

    public AdminLoginHandler(AdminLoginAttemptService lockout, AdminAuditLogService audit) {
        this.lockout = lockout;
        this.audit = audit;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException {
        String ip = req.getRemoteAddr();
        lockout.resetAttempts(auth.getName(), ip);
        audit.record(auth.getName(), "LOGIN_SUCCESS", null, null, ip);
        res.sendRedirect("/admin/dashboard");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res,
                                        AuthenticationException ex) throws IOException {
        String username = req.getParameter("username");
        String ip = req.getRemoteAddr();
        if (username != null) {
            lockout.recordFailure(username, ip);
            audit.record(username, "LOGIN_FAILURE", null, ex.getMessage(), ip);
        }
        res.sendRedirect("/admin/login?error");
    }
}
