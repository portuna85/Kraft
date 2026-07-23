package com.kraft.community.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kraft.common.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * community 체인은 Next.js가 별도로 페이지를 서빙하므로(백엔드는 API만 응답), 미인증 요청은
 * 항상 401 JSON으로 응답한다 — 로그인 페이지로의 리다이렉트는 프런트 라우팅(4단계)이 담당한다.
 * GlobalExceptionHandler와 동일한 ApiErrorResponse 계약을 쓰지만, 이 지점은
 * ExceptionTranslationFilter가 DispatcherServlet 이전에 처리하므로 직접 응답을 작성한다.
 */
@Component
public class CommunityAuthEntryPoint implements AuthenticationEntryPoint {

    // 앱 전역 ObjectMapper 빈에 의존하지 않는다 — Boot 4.1 기본 JacksonAutoConfiguration은
    // com.fasterxml.jackson.databind.ObjectMapper 타입 빈을 노출하지 않는다(내부적으로 다른
    // JsonMapper 계열을 우선 구성). 이 클래스는 소규모 고정 DTO 직렬화만 필요하므로 로컬
    // 인스턴스로 충분하다.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "COMMUNITY_LOGIN_REQUIRED",
                "로그인이 필요합니다.",
                request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
