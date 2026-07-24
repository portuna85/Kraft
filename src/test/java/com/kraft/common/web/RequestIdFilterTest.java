package com.kraft.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RequestIdFilter 요청 경로 로깅")
class RequestIdFilterTest {

    @Test
    @DisplayName("OAuth code와 state가 포함된 query string을 로그 경로에서 제외한다")
    void excludesQueryStringFromLoggedPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/naver");

        String path = RequestIdFilter.requestPath(request);

        assertThat(path).isEqualTo("/login/oauth2/code/naver");
        verify(request, never()).getQueryString();
    }
}
