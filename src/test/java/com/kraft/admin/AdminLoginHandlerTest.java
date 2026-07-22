package com.kraft.admin;

import com.kraft.common.web.ClientIpResolver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 로그인 핸들러 테스트")
class AdminLoginHandlerTest {

    @Mock
    private AdminLoginAttemptService lockout;
    @Mock
    private AdminAuditLogService audit;
    @Mock
    private ClientIpResolver ipResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private SimpleMeterRegistry meterRegistry;
    private AdminLoginHandler handler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        handler = new AdminLoginHandler(lockout, audit, ipResolver, meterRegistry);
    }

    @Test
    @DisplayName("150자 username 로그인 실패도 100자로 잘라 기록하고 예외 없이 리다이렉트한다")
    void onAuthenticationFailure_overlongUsername_truncatedAndRedirects() throws Exception {
        when(request.getParameter("username")).thenReturn("x".repeat(150));
        when(ipResolver.resolve(request)).thenReturn("1.2.3.4");

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        ArgumentCaptor<String> auditUser = ArgumentCaptor.forClass(String.class);
        verify(audit).record(auditUser.capture(), eq("LOGIN_FAILURE"), isNull(), anyString(), eq("1.2.3.4"));
        assertThat(auditUser.getValue()).hasSize(100);
        verify(lockout).recordFailure(auditUser.getValue(), "1.2.3.4");
        verify(response).sendRedirect("/admin/login?error");
        assertThat(meterRegistry.get("kraft_admin_login_failures_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("공백뿐인 username은 기록·잠금 없이 리다이렉트만 한다")
    void onAuthenticationFailure_blankUsername_skipsRecording() throws Exception {
        when(request.getParameter("username")).thenReturn("   ");
        when(ipResolver.resolve(request)).thenReturn("1.2.3.4");

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        verify(lockout, never()).recordFailure(anyString(), anyString());
        verify(audit, never()).record(any(), any(), any(), any(), any());
        verify(response).sendRedirect("/admin/login?error");
        assertThat(meterRegistry.get("kraft_admin_login_failures_total").counter().count()).isZero();
    }
}
