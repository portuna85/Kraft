package com.kraft.common.web;

import com.kraft.common.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("클라이언트 IP 확인 테스트")
class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        SecurityProperties props = mock(SecurityProperties.class);
        when(props.trustedProxyCidr()).thenReturn("172.28.0.0/16");
        resolver = new ClientIpResolver(props);
    }

    @Test
    @DisplayName("XFF 헤더가 없을 때 RemoteAddr을 반환하는지 확인")
    void resolve_returnsRemoteAddr_whenNoXff() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("203.0.113.5");

        assertThat(resolver.resolve(req)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("XFF 헤더가 비어있을 때 RemoteAddr을 반환하는지 확인")
    void resolve_returnsRemoteAddr_whenXffBlank() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("  ");
        when(req.getRemoteAddr()).thenReturn("203.0.113.5");

        assertThat(resolver.resolve(req)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("신뢰할 수 없는 단일 XFF IP가 있을 때 해당 IP를 반환하는지 확인")
    void resolve_returnsClientIp_whenSingleXffNotTrusted() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("172.28.0.1"); // trusted proxy peer
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");

        assertThat(resolver.resolve(req)).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("XFF 체인에서 신뢰할 수 있는 프록시 IP를 건너뛰는지 확인")
    void resolve_skipsTrustedProxy_inXffChain() {
        // XFF: client → proxy1(trusted) → caddy(trusted)
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("172.28.0.1"); // trusted proxy peer
        when(req.getHeader("X-Forwarded-For"))
                .thenReturn("203.0.113.10, 172.28.0.10, 172.28.0.1");

        // Right-to-left: 172.28.0.1 (trusted) → 172.28.0.10 (trusted) → 203.0.113.10 (not trusted)
        assertThat(resolver.resolve(req)).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("모든 XFF IP가 신뢰할 수 있는 경우 마지막 IP를 반환하는지 확인")
    void resolve_returnsLastTrustedIp_whenAllXffAreTrusted() {
        // All IPs in XFF are in trusted CIDR — return leftmost (first untrusted not found)
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For"))
                .thenReturn("172.28.1.1, 172.28.0.2");
        when(req.getRemoteAddr()).thenReturn("172.28.0.1");

        // All are trusted, loop exhausts → falls through to remoteAddr
        assertThat(resolver.resolve(req)).isEqualTo("172.28.0.1");
    }

    @Test
    @DisplayName("CIDR 범위 내의 주소를 신뢰할 수 있는 프록시로 인식하는지 확인")
    void isTrustedProxy_trueForAddressInCidr() {
        assertThat(resolver.isTrustedProxy("172.28.0.1")).isTrue();
        assertThat(resolver.isTrustedProxy("172.28.255.255")).isTrue();
    }

    @Test
    @DisplayName("CIDR 범위 밖의 주소를 신뢰할 수 없는 프록시로 인식하는지 확인")
    void isTrustedProxy_falseForAddressOutsideCidr() {
        assertThat(resolver.isTrustedProxy("172.29.0.1")).isFalse();
        assertThat(resolver.isTrustedProxy("10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("호스트 이름에 대해 신뢰할 수 없는 프록시로 인식하는지 확인")
    void isTrustedProxy_falseForHostname() {
        // B-2: InetAddress.ofLiteral() rejects hostnames — must return false
        assertThat(resolver.isTrustedProxy("evil.example.com")).isFalse();
    }

    @Test
    @DisplayName("다른 주소 체계(IPv6 vs IPv4)에 대해 신뢰할 수 없는 프록시로 인식하는지 확인")
    void isTrustedProxy_falseForMixedAddressFamily() {
        // IPv6 address vs IPv4 CIDR — different byte lengths → false
        assertThat(resolver.isTrustedProxy("::1")).isFalse();
    }
}
