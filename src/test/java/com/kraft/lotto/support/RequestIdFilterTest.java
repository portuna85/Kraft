package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("요청 ID 필터")
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    @DisplayName("헤더가 없으면 요청 ID를 생성한다")
    void generatesRequestIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new AssertingChain());

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isNotBlank();
        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("정화된 요청 ID 헤더를 재사용한다")
    void reusesSanitizedHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/token/secret");
        request.addHeader(RequestIdFilter.HEADER_NAME, "rid-123\r\nbad");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new AssertingChain());

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("rid-123__bad");
    }

    private static class AssertingChain extends MockFilterChain {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                             jakarta.servlet.ServletResponse response) throws IOException, ServletException {
            assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNotBlank();
            assertThat(MDC.get(RequestIdFilter.MDC_METHOD)).isEqualTo("GET");
            assertThat(MDC.get(RequestIdFilter.MDC_PATH)).doesNotContain("secret");
        }
    }
}
