package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("운영 도구 접근 필터 테스트")
class OpsAccessFilterTest {

    @Test
    @DisplayName("허용 IP 목록에 공백 항목이 있으면 조용히 무시한다")
    void ignoresBlankAllowedIpEntries() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1", "  "));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/fetch-logs/failures");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Ops-Token", "expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("ops.enabled=false 이면 토큰 없이도 모든 요청을 통과시킨다")
    void allowsAllRequestsWhenOpsDisabled() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setEnabled(false);
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/fetch-logs/failures");
        request.setRemoteAddr("1.2.3.4");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("required-token이 비어 있으면 토큰 검증을 건너뛴다")
    void allowsAccessWhenTokenNotConfigured() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        // requiredToken 기본값 "" → isBlank()=true → 검증 건너뜀
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/fetch-logs/failures");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("헤더 토큰이 있으면 쿠키보다 우선된다")
    void headerTokenHasPriorityOverCookie() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/fetch-logs/failures");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Ops-Token", "invalid-token");
        request.setCookies(new Cookie("KRAFT_OPS_TOKEN", "expected-token"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("쿼리 파라미터 토큰만 있으면 인증이 거부된다")
    void queryParamTokenAloneIsRejected() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ops");
        request.setRemoteAddr("127.0.0.1");
        request.setParameter("opsToken", "expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("헤더가 없으면 쿠키 토큰으로 인증한다")
    void usesCookieTokenWhenHeaderIsMissing() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ops");
        request.setRemoteAddr("127.0.0.1");
        request.setCookies(new Cookie("KRAFT_OPS_TOKEN", "expected-token"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("POST 요청에 쿠키 토큰만 있으면 인증이 거부된다")
    void postWithCookieOnlyTokenIsRejected() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ops/collect");
        request.setRemoteAddr("127.0.0.1");
        request.setCookies(new Cookie("KRAFT_OPS_TOKEN", "expected-token"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("POST 요청에 빈 헤더 토큰만 있으면 인증이 거부된다")
    void postWithBlankHeaderTokenIsRejected() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ops/collect");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Ops-Token", "   ");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("POST 요청에 헤더 토큰이 틀리면 인증이 거부된다")
    void postWithWrongHeaderTokenIsRejected() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ops/collect");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Ops-Token", "wrong-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("POST 요청에 헤더 토큰이 있으면 인증에 성공한다")
    void postWithHeaderTokenIsAccepted() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ops/collect");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Ops-Token", "expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("인증에 성공하면 필터 체인이 계속 진행된다 (감사 로그 기록 시점)")
    void successfulAccessProceedsToFilterChain() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/fetch-logs/failures");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Ops-Token", "expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("trustForwardedFor=true 이면 X-Forwarded-For 첫 IP로 허용 여부를 판단한다")
    void usesForwardedForWhenTrusted() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setTrustForwardedFor(true);
        properties.getOps().setAllowedIps(java.util.List.of("198.51.100.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ops");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.10");
        request.addHeader("X-Ops-Token", "expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
