package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@DisplayName("Admin 인증 핸들러 테스트")
class AdminAuthHandlerTest {

    AdminAuditLogService auditService;

    @BeforeEach
    void setUp() {
        auditService = mock(AdminAuditLogService.class);
    }

    @Test
    @DisplayName("SuccessHandler — 인증 성공 시 /admin/ops로 리다이렉트한다")
    void successHandlerRedirectsToOps() throws Exception {
        AdminAuthSuccessHandler handler = new AdminAuthSuccessHandler(auditService);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/ops");
        verify(auditService).recordSuccess(eq("admin"), eq("LOGIN_SUCCESS"),
                isNull(), any(), any());
    }

    @Test
    @DisplayName("SuccessHandler — auditService가 null이어도 NPE 없이 동작한다")
    void successHandlerWorksWithoutAuditService() throws Exception {
        AdminAuthSuccessHandler handler = new AdminAuthSuccessHandler(null);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/ops");
    }

    @Test
    @DisplayName("FailureHandler — /admin/login?error로 리다이렉트하고 감사 로그를 기록한다")
    void failureHandlerRedirectsAndAudits() throws Exception {
        AdminAuthFailureHandler handler = new AdminAuthFailureHandler(auditService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        org.springframework.security.core.AuthenticationException ex =
                new org.springframework.security.authentication.BadCredentialsException("bad");

        handler.onAuthenticationFailure(request, response, ex);

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/login?error");
        verify(auditService).recordFailure(eq("unknown"), eq("LOGIN_FAILED"),
                isNull(), any(), any(), eq("bad"));
    }

    @Test
    @DisplayName("FailureHandler — auditService가 null이어도 NPE 없이 동작한다")
    void failureHandlerWorksWithoutAuditService() throws Exception {
        AdminAuthFailureHandler handler = new AdminAuthFailureHandler(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        org.springframework.security.core.AuthenticationException ex =
                new org.springframework.security.authentication.BadCredentialsException("bad");

        handler.onAuthenticationFailure(request, response, ex);

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/login?error");
    }
}
