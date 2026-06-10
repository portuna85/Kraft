package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("보안 헤더 필터")
class SecurityHeadersFilterTest {

    @Test
    @DisplayName("응답 보안 헤더를 추가한다")
    void addsSecurityHeaders() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Content-Security-Policy")).isNotBlank();
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    @DisplayName("전송 보안 헤더 활성화 시 전송 보안 헤더 헤더를 추가한다")
    void hstsHeaderIsAddedWhenEnabled() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getHeaders().setHstsEnabled(true);
        properties.getHeaders().setHstsMaxAgeSeconds(31536000L);
        properties.getHeaders().setHstsIncludeSubDomains(true);
        SecurityHeadersFilter filter = new SecurityHeadersFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    @DisplayName("전송 보안 헤더 비활성화 시 전송 보안 헤더 헤더를 추가하지 않는다")
    void hstsHeaderIsAbsentWhenDisabled() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    @DisplayName("요청마다 콘텐츠 보안 정책 논스를 생성해 요청 속성과 콘텐츠 보안 정책 헤더에 포함한다")
    void nonceIsInjectedIntoCspAndRequestAttribute() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String nonce = (String) request.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTRIBUTE);
        assertThat(nonce).isNotBlank();
        assertThat(response.getHeader("Content-Security-Policy")).contains("'nonce-" + nonce + "'");
    }

    @Test
    @DisplayName("두 요청의 논스는 서로 다르다")
    void eachRequestGetsDifferentNonce() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(properties);

        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/");
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/");
        filter.doFilter(req1, new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(req2, new MockHttpServletResponse(), new MockFilterChain());

        String nonce1 = (String) req1.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTRIBUTE);
        String nonce2 = (String) req2.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTRIBUTE);
        assertThat(nonce1).isNotEqualTo(nonce2);
    }

    @ParameterizedTest
    @DisplayName("논스 주입은 스크립트 출처 지시자에 논스를 주입한다")
    @ValueSource(strings = {
            "default-src 'self'; script-src 'self'; style-src 'self'",
            "script-src 'self' https://cdn.example.com; object-src 'none'"
    })
    void injectNonceAddsNonceToScriptSrc(String csp) {
        String result = SecurityHeadersFilter.injectNonce(csp, "abc123");

        assertThat(result).contains("'nonce-abc123'");
        assertThat(result).contains("script-src 'nonce-abc123'");
    }
}
