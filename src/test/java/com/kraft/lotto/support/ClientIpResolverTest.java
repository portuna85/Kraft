package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("클라이언트 아이피 리졸버")
class ClientIpResolverTest {

    @Test
    @DisplayName("원격 주소가 신뢰 프록시 사이더에 속하면 오른쪽 첫 비신뢰 아이피를 사용한다")
    void resolvesFromForwardedForWhenRemoteIsTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", " 198.51.100.1 , 198.51.100.2 ");

        String resolved = ClientIpResolver.resolve(request, List.of("10.0.0.0/8"));

        assertThat(resolved).isEqualTo("198.51.100.2");
    }

    @Test
    @DisplayName("원격 주소가 신뢰 프록시 사이더에 속하지 않으면 원격 주소를 사용한다")
    void resolvesFromRemoteAddrWhenNotTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1");

        String resolved = ClientIpResolver.resolve(request, List.of("10.0.0.0/8"));

        assertThat(resolved).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("신뢰 프록시 목록이 비어 있으면 원격 주소를 사용한다")
    void resolvesFromRemoteAddrWhenTrustedProxyListIsEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1");

        String resolved = ClientIpResolver.resolve(request, List.of());

        assertThat(resolved).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("신뢰 프록시 경유이지만 전달 아이피 헤더가 없으면 원격 주소를 사용한다")
    void fallsBackToRemoteAddrWhenForwardedForHeaderAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("10.0.0.1");

        String resolved = ClientIpResolver.resolve(request, List.of("10.0.0.0/8"));

        assertThat(resolved).isEqualTo("10.0.0.1");
    }
}
