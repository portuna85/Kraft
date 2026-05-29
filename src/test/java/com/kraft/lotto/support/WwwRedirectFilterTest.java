package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("WWW 리다이렉트 필터")
class WwwRedirectFilterTest {

    private final WwwRedirectFilter filter = new WwwRedirectFilter();

    @Test
    @DisplayName("non-www 도메인 요청을 www로 301 리다이렉트한다")
    void redirectsNonWwwToWww() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("kraft.io.kr");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(301);
        assertThat(response.getHeader("Location")).isEqualTo("https://www.kraft.io.kr/");
    }

    @Test
    @DisplayName("경로와 쿼리스트링을 보존하여 리다이렉트한다")
    void preservesPathAndQueryString() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/some/path");
        request.setServerName("kraft.io.kr");
        request.setQueryString("a=1&b=2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Location")).isEqualTo("https://www.kraft.io.kr/some/path?a=1&b=2");
    }

    @Test
    @DisplayName("www 도메인 요청은 통과시킨다")
    void passesThroughWwwRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("www.kraft.io.kr");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @ParameterizedTest
    @DisplayName("로컬/내부 호스트는 리다이렉트하지 않는다")
    @ValueSource(strings = {"localhost", "127.0.0.1", "192.168.0.1", "10.0.0.1"})
    void doesNotRedirectLocalHosts(String host) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName(host);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
