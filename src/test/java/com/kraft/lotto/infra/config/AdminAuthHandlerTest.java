package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

@DisplayName("Admin 인증 핸들러 테스트")
class AdminAuthHandlerTest {

    AdminAuditLogService auditService;

    @BeforeEach
    void setUp() {
        auditService = mock(AdminAuditLogService.class);
    }

    @Test
    @DisplayName("SuccessHandler — 허용된 이메일이면 /admin/ops로 리다이렉트한다")
    void successHandlerRedirectsAllowedEmail() throws Exception {
        AdminAuthSuccessHandler handler =
                new AdminAuthSuccessHandler(List.of("admin@example.com"), auditService);

        Authentication auth = mockOAuth2Auth("admin@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/ops");
        verify(auditService).recordSuccess(eq("admin@example.com"), eq("LOGIN_SUCCESS"),
                isNull(), any(), any());
    }

    @Test
    @DisplayName("SuccessHandler — 허용되지 않은 이메일이면 /admin/login?error=unauthorized로 리다이렉트한다")
    void successHandlerRejectsUnauthorizedEmail() throws Exception {
        AdminAuthSuccessHandler handler =
                new AdminAuthSuccessHandler(List.of("admin@example.com"), auditService);

        Authentication auth = mockOAuth2Auth("other@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(mock(HttpSession.class));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/login?error=unauthorized");
        verify(auditService).recordFailure(eq("other@example.com"), eq("LOGIN_DENIED"),
                isNull(), any(), any(), any());
    }

    @Test
    @DisplayName("SuccessHandler — auditService가 null이어도 NPE 없이 동작한다")
    void successHandlerWorksWithoutAuditService() throws Exception {
        AdminAuthSuccessHandler handler =
                new AdminAuthSuccessHandler(List.of("admin@example.com"), null);

        Authentication auth = mockOAuth2Auth("admin@example.com");
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

    private static Authentication mockOAuth2Auth(String email) {
        var attributes = Map.<String, Object>of("email", email, "sub", "12345");
        var authority = new OAuth2UserAuthority(attributes);
        var principal = new DefaultOAuth2User(List.of(authority), attributes, "email");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        return auth;
    }
}
