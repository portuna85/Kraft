package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("네비게이션 모델 어드바이스")
class NavModelAdviceTest {

    @Mock
    HttpServletRequest request;

    NavModelAdvice advice = new NavModelAdvice();

    @Test
    @DisplayName("요청 URI를 currentPath로 반환한다")
    void returnsRequestUri() {
        when(request.getRequestURI()).thenReturn("/frequency");
        assertThat(advice.currentPath(request)).isEqualTo("/frequency");
    }

    @Test
    @DisplayName("URI가 null이면 루트 경로를 반환한다")
    void returnsRootWhenUriIsNull() {
        when(request.getRequestURI()).thenReturn(null);
        assertThat(advice.currentPath(request)).isEqualTo("/");
    }
}
