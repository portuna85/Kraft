package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("공개 경로 속도 제한 필터")
class PublicRateLimitFilterTest {

    @Test
    @DisplayName("공개 프래그먼트 경로에서 요청 한도를 초과하면 429를 반환한다")
    void returnsTooManyRequestsAfterThreshold() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setMaxRequests(2);
        properties.getRateLimit().setWindowSeconds(60);
        PublicRateLimitFilter filter = new PublicRateLimitFilter(properties);

        MockHttpServletResponse first = execute(filter, "/fragments/recommend", "198.51.100.11");
        MockHttpServletResponse second = execute(filter, "/fragments/recommend", "198.51.100.11");
        MockHttpServletResponse third = execute(filter, "/fragments/recommend", "198.51.100.11");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isNotBlank();
        assertThat(third.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(third.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(third.getHeader("X-RateLimit-Reset")).isNotBlank();
    }

    @Test
    @DisplayName("공개 경로가 아닌 경우 속도 제한을 적용하지 않는다")
    void doesNotRateLimitNonPublicPaths() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setMaxRequests(1);
        properties.getRateLimit().setWindowSeconds(60);
        PublicRateLimitFilter filter = new PublicRateLimitFilter(properties);

        MockHttpServletResponse first = execute(filter, "/css/app.css", "198.51.100.11");
        MockHttpServletResponse second = execute(filter, "/css/app.css", "198.51.100.11");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(first.getHeader("X-RateLimit-Limit")).isNull();
        assertThat(second.getHeader("X-RateLimit-Limit")).isNull();
    }

    @Test
    @DisplayName("카운터 캐시 키 수는 설정된 상한을 넘지 않는다")
    void boundsCounterCacheSize() throws Exception {
        KraftSecurityProperties properties = new KraftSecurityProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setMaxRequests(1);
        properties.getRateLimit().setWindowSeconds(60);
        properties.getRateLimit().setMaxKeys(32);
        PublicRateLimitFilter filter = new PublicRateLimitFilter(properties);

        for (int i = 0; i < 2_000; i++) {
            execute(filter, "/fragments/recommend", "198.51.100." + i);
        }

        filter.cleanUpCounters();
        assertThat(filter.estimatedCounterSize()).isLessThanOrEqualTo(32);
    }

    private static MockHttpServletResponse execute(PublicRateLimitFilter filter,
                                                   String path,
                                                   String remoteAddr) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
