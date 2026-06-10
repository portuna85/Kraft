package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("운영 접근 제어 필터 테스트")
class OpsAccessFilterTest {

    @Test
    @DisplayName("헤더 토큰이 유효하지 않으면 인증을 거부한다")
    void invalidHeaderTokenIsRejected() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/fetch-logs/failures");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Ops-Token", "invalid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("쿼리 파라미터 토큰만 있으면 인증을 거부한다")
    void queryParamTokenAloneIsRejected() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/collect/status");
        request.setRemoteAddr("127.0.0.1");
        request.setParameter("opsToken", "expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("헤더 없이 쿠키 토큰만 있으면 인증을 거부한다")
    void cookieOnlyTokenIsRejected() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/collect/status");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Cookie", "KRAFT_OPS_TOKEN=expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("등록 요청 요청도 헤더 토큰이 유효하면 인증에 성공한다")
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
    @DisplayName("인증 전달자 토큰이 유효하면 인증에 성공한다")
    void bearerTokenIsAccepted() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/fetch-logs/failures");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Authorization", "Bearer expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("인증 성공 시 필터 체인을 계속 진행한다")
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
    @DisplayName("신뢰 프록시면 전달 아이피 헤더의 첫 아이피를 기준으로 허용 여부를 판단한다")
    void usesForwardedForWhenTrusted() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.setTrustedProxies(java.util.List.of("203.0.113.10"));
        properties.getOps().setAllowedIps(java.util.List.of("198.51.100.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/collect/status");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.10");
        request.addHeader("X-Ops-Token", "expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("신뢰 프록시가 아니면 전달 아이피 헤더를 신뢰하지 않는다")
    void ignoresForwardedForWhenRemoteIsNotTrustedProxy() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.setTrustedProxies(java.util.List.of("203.0.113.10"));
        properties.getOps().setAllowedIps(java.util.List.of("198.51.100.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ops/collect/status");
        request.setRemoteAddr("203.0.113.11");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.11");
        request.addHeader("X-Ops-Token", "expected-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("관리자 운영 접두사만 포함한 경로는 필터 대상이 아니다")
    void nonOpsAdminPrefixPathIsNotFiltered() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ops-panel");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("관리자 운영 경로는 스프링 시큐리티에 위임하므로 운영 접근 필터 대상이 아니다")
    void adminOpsPathIsNotFilteredByOpsFilter() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        properties.getOps().setRequiredToken("expected-token");
        OpsAccessFilter filter = new OpsAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ops/history");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
