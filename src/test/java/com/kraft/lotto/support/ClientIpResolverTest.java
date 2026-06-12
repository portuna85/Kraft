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

    @Test
    @DisplayName("다중 홉: 모든 전달 아이피가 신뢰 프록시이면 가장 왼쪽 아이피를 반환한다")
    void allForwardedIpsTrustedReturnsLeftmost() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("10.0.0.3");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");

        String resolved = ClientIpResolver.resolve(request, List.of("10.0.0.0/8"));

        assertThat(resolved).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("다중 홉: 신뢰 프록시가 연속으로 이어진 경우 오른쪽에서 첫 번째 비신뢰 아이피를 반환한다")
    void multiHopPicksRightmostUntrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("10.0.0.3");
        // 왼쪽: 실제 클라이언트(비신뢰), 오른쪽: 신뢰 프록시들
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1, 10.0.0.2");

        String resolved = ClientIpResolver.resolve(request, List.of("10.0.0.0/8"));

        assertThat(resolved).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("전달 아이피 헤더가 공백 문자열이면 원격 주소를 사용한다")
    void fallsBackToRemoteAddrWhenForwardedForHeaderIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "   ");

        String resolved = ClientIpResolver.resolve(request, List.of("10.0.0.0/8"));

        assertThat(resolved).isEqualTo("10.0.0.1");
    }
}
