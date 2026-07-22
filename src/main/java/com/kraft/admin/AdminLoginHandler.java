package com.kraft.admin;

import com.kraft.common.web.ClientIpResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final ClientIpResolver ipResolver;
    private final Counter loginFailureCounter;

    public AdminLoginHandler(AdminLoginAttemptService lockout, AdminAuditLogService audit,
                             ClientIpResolver ipResolver, MeterRegistry meterRegistry) {
        this.lockout = lockout;
        this.audit = audit;
        this.ipResolver = ipResolver;
        this.loginFailureCounter = Counter.builder("kraft_admin_login_failures_total")
                .description("관리자 로그인 실패 횟수")
                .register(meterRegistry);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException {
        String ip = ipResolver.resolve(req);
        lockout.resetAttempts(auth.getName(), ip);
        audit.record(auth.getName(), "LOGIN_SUCCESS", null, null, ip);
        res.sendRedirect("/admin/dashboard");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res,
                                        AuthenticationException ex) throws IOException {
        // 감사 로그(VARCHAR(100))와 잠금 캐시가 같은 키를 보도록 서비스와 동일 규칙으로 정규화한다.
        String username = AdminLoginAttemptService.normalizeUsername(req.getParameter("username"));
        String ip = ipResolver.resolve(req);
        if (username != null && !username.isEmpty()) {
            lockout.recordFailure(username, ip);
            audit.record(username, "LOGIN_FAILURE", null, ex.getMessage(), ip);
            loginFailureCounter.increment();
        }
        res.sendRedirect("/admin/login?error");
    }
}
