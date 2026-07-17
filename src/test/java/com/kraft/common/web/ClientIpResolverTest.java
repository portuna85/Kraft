package com.kraft.common.web;

import com.kraft.common.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("클라이언트 주소 해석기 테스트")
class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        SecurityProperties props = mock(SecurityProperties.class);
        when(props.trustedProxyCidr()).thenReturn("172.16.0.0/12,10.0.0.0/8");
        resolver = new ClientIpResolver(props);
    }

    @Test
    @DisplayName("전달 주소 헤더가 없으면 원격 주소를 반환한다")
    void resolve_returnsRemoteAddr_whenNoXff() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("203.0.113.5");

        assertThat(resolver.resolve(req)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("전달 주소 헤더가 비어 있으면 원격 주소를 반환한다")
    void resolve_returnsRemoteAddr_whenXffBlank() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("  ");
        when(req.getRemoteAddr()).thenReturn("203.0.113.5");

        assertThat(resolver.resolve(req)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("단일 전달 주소가 비신뢰 주소면 해당 주소를 반환한다")
    void resolve_returnsClientIp_whenSingleXffNotTrusted() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("172.20.0.2");
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");

        assertThat(resolver.resolve(req)).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("전달 주소 체인에서는 신뢰 프록시를 건너뛴다")
    void resolve_skipsTrustedProxy_inXffChain() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("172.20.0.2");
        when(req.getHeader("X-Forwarded-For"))
                .thenReturn("203.0.113.10, 10.0.0.5, 172.20.0.2");

        assertThat(resolver.resolve(req)).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("모든 전달 주소가 신뢰 주소면 원격 주소를 반환한다")
    void resolve_returnsRemoteAddr_whenAllXffAreTrusted() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For"))
                .thenReturn("10.1.1.1, 172.20.0.3");
        when(req.getRemoteAddr()).thenReturn("172.20.0.2");

        assertThat(resolver.resolve(req)).isEqualTo("172.20.0.2");
    }

    @Test
    @DisplayName("여러 신뢰 대역 중 하나에 포함되면 신뢰 프록시로 본다")
    void isTrustedProxy_trueWhenIpMatchesAnyConfiguredCidr() {
        assertThat(resolver.isTrustedProxy("172.20.0.2")).isTrue();
        assertThat(resolver.isTrustedProxy("10.10.10.10")).isTrue();
    }

    @Test
    @DisplayName("모든 신뢰 대역 밖 주소는 비신뢰 프록시다")
    void isTrustedProxy_falseWhenIpMatchesNoConfiguredCidr() {
        assertThat(resolver.isTrustedProxy("192.168.0.10")).isFalse();
        assertThat(resolver.isTrustedProxy("203.0.113.1")).isFalse();
    }

    @Test
    @DisplayName("호스트명은 신뢰 프록시로 보지 않는다")
    void isTrustedProxy_falseForHostname() {
        assertThat(resolver.isTrustedProxy("evil.example.com")).isFalse();
    }

    @Test
    @DisplayName("주소 체계가 다르면 신뢰 프록시로 보지 않는다")
    void isTrustedProxy_falseForMixedAddressFamily() {
        assertThat(resolver.isTrustedProxy("::1")).isFalse();
    }

    @Test
    @DisplayName("형식이 잘못된 CIDR 설정은 기동 시(생성자에서) 예외를 던진다")
    void constructor_throwsIllegalState_whenCidrFormatInvalid() {
        SecurityProperties props = mock(SecurityProperties.class);
        when(props.trustedProxyCidr()).thenReturn("bad-cidr, 172.16.0.0/12");

        assertThatThrownBy(() -> new ClientIpResolver(props))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("프리픽스 길이가 범위를 벗어난 CIDR 설정은 기동 시 예외를 던진다")
    void constructor_throwsIllegalState_whenPrefixLengthOutOfRange() {
        SecurityProperties props = mock(SecurityProperties.class);
        when(props.trustedProxyCidr()).thenReturn("172.16.0.0/33");

        assertThatThrownBy(() -> new ClientIpResolver(props))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("유효한 CIDR만 있으면 기동 시 예외 없이 정상 동작한다")
    void constructor_succeeds_whenAllCidrsValid() {
        SecurityProperties props = mock(SecurityProperties.class);
        when(props.trustedProxyCidr()).thenReturn("172.16.0.0/12, 10.0.0.0/8, ::1/128");

        ClientIpResolver resolverWithValidEntries = new ClientIpResolver(props);

        assertThat(resolverWithValidEntries.isTrustedProxy("172.20.0.2")).isTrue();
        assertThat(resolverWithValidEntries.isTrustedProxy("203.0.113.1")).isFalse();
    }
}
