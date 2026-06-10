package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("액추에이터 접근 제어 필터")
class ActuatorAccessFilterTest {

    @Test
    @DisplayName("허용된 아이피로부터의 액추에이터 접근을 허용한다")
    void allowsAllowlistedIp() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getActuator().setAllowedIps(java.util.List.of("127.0.0.1"));
        ActuatorAccessFilter filter = new ActuatorAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("허용되지 않은 아이피로부터의 액추에이터 접근을 차단한다")
    void blocksNonAllowlistedIp() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getActuator().setAllowedIps(java.util.List.of("127.0.0.1"));
        ActuatorAccessFilter filter = new ActuatorAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("액추에이터 경로가 아닌 경우 아이피 체크 없이 허용한다")
    void ignoresNonActuatorPaths() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getActuator().setAllowedIps(java.util.List.of("127.0.0.1"));
        ActuatorAccessFilter filter = new ActuatorAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("신뢰 프록시가 아니면 전달 아이피 헤더를 신뢰하지 않는다")
    void ignoresForwardedForWhenRemoteProxyIsNotTrusted() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.setTrustedProxies(java.util.List.of("203.0.113.10"));
        properties.getActuator().setAllowedIps(java.util.List.of("198.51.100.1"));
        ActuatorAccessFilter filter = new ActuatorAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("203.0.113.20");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.20");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("신뢰 프록시이면 전달 아이피 헤더 첫 값을 기준으로 허용한다")
    void usesForwardedForWhenRemoteProxyIsTrusted() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.setTrustedProxies(java.util.List.of("203.0.113.10"));
        properties.getActuator().setAllowedIps(java.util.List.of("198.51.100.1"));
        ActuatorAccessFilter filter = new ActuatorAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("후행 슬래시가 없는 액추에이터 루트 경로도 보호된다")
    void blocksNonAllowlistedIpForActuatorRoot() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getActuator().setAllowedIps(java.util.List.of("127.0.0.1"));
        ActuatorAccessFilter filter = new ActuatorAccessFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }
}
