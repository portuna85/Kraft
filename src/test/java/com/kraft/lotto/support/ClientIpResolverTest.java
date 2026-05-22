package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("ClientIpResolver")
class ClientIpResolverTest {

    @Test
    @DisplayName("trustForwardedFor=true 이면 X-Forwarded-For 첫 번째 유효 IP를 사용한다")
    void resolvesFromForwardedForWhenTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", " 198.51.100.1 , 198.51.100.2 ");

        String resolved = ClientIpResolver.resolve(request, true);

        assertThat(resolved).isEqualTo("198.51.100.1");
    }

    @Test
    @DisplayName("trustForwardedFor=false 이면 remoteAddr를 사용한다")
    void resolvesFromRemoteAddrWhenNotTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1");

        String resolved = ClientIpResolver.resolve(request, false);

        assertThat(resolved).isEqualTo("203.0.113.10");
    }
}
