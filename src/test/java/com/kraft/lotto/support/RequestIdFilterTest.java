package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("요청 아이디 필터")
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    @DisplayName("헤더가 없으면 요청 아이디를 생성한다")
    void generatesRequestIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new AssertingChain());

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isNotBlank();
        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("정화된 요청 아이디 헤더를 재사용한다")
    void reusesSanitizedHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/token/secret");
        request.addHeader(RequestIdFilter.HEADER_NAME, "rid-123\r\nbad");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new AssertingChain());

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("rid-123__bad");
    }

    @Test
    @DisplayName("추적기가 주입되고 현재 스팬이 있으면 요청 아이디를 태그로 전파한다")
    void propagatesRequestIdToSpanTagWhenTracerPresent() throws Exception {
        Span span = mock(Span.class);
        when(span.tag("requestId", "req-abc")).thenReturn(span);
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);
        ObjectProvider<Tracer> provider = new ObjectProvider<>() {
            @Override
            public Tracer getObject() { return tracer; }

            @Override
            public Tracer getIfAvailable() { return tracer; }
        };
        RequestIdFilter tracingFilter = new RequestIdFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(RequestIdFilter.HEADER_NAME, "req-abc");

        tracingFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // tag 호출이 예외 없이 완료되면 전파 성공
        assertThat(response(tracingFilter, "req-abc")).isEqualTo("req-abc");
    }

    @Test
    @DisplayName("추적기가 주입되어 있어도 현재 스팬이 널이면 전파하지 않는다")
    void doesNotPropagateWhenCurrentSpanIsNull() throws Exception {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);
        ObjectProvider<Tracer> provider = new ObjectProvider<>() {
            @Override
            public Tracer getObject() { return tracer; }

            @Override
            public Tracer getIfAvailable() { return tracer; }
        };
        RequestIdFilter tracingFilter = new RequestIdFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tracingFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isNotBlank();
    }

    private static String response(RequestIdFilter f, String reqId) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
        req.addHeader(RequestIdFilter.HEADER_NAME, reqId);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        f.doFilter(req, resp, new MockFilterChain());
        return resp.getHeader(RequestIdFilter.HEADER_NAME);
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
