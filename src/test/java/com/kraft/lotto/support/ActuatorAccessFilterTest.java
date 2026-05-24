package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("Actuator 접근 제어 필터")
class ActuatorAccessFilterTest {

    @Test
    @DisplayName("허용된 IP로부터의 Actuator 접근을 허용한다")
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
    @DisplayName("허용되지 않은 IP로부터의 Actuator 접근을 차단한다")
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
    @DisplayName("Actuator 경로가 아닌 경우 IP 체크 없이 허용한다")
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
    @DisplayName("trusted proxy가 아니면 X-Forwarded-For를 신뢰하지 않는다")
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
    @DisplayName("trusted proxy이면 X-Forwarded-For 첫 값을 기준으로 허용한다")
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
}
