package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
    @DisplayName("HSTS 활성화 시 Strict-Transport-Security 헤더를 추가한다")
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
    @DisplayName("HSTS 비활성화 시 Strict-Transport-Security 헤더를 추가하지 않는다")
    void hstsHeaderIsAbsentWhenDisabled() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    }
}
