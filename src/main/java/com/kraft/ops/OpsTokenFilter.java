package com.kraft.ops;

import com.kraft.common.config.OpsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(5)
public class OpsTokenFilter extends OncePerRequestFilter {

    private static final String UNAUTHORIZED_BODY =
            "{\"code\":\"OPS_UNAUTHORIZED\",\"message\":\"운영 API 인증에 실패했습니다.\"}";
    private static final String DISABLED_BODY =
            "{\"code\":\"OPS_DISABLED\",\"message\":\"운영 API 토큰이 설정되지 않았습니다.\"}";

    private final OpsProperties opsProperties;

    public OpsTokenFilter(OpsProperties opsProperties) {
        this.opsProperties = opsProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/ops");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String expected = opsProperties.token();
        if (expected == null || expected.isBlank()) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, DISABLED_BODY);
            return;
        }
        String token = request.getHeader("X-Ops-Token");
        if (token == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            writeError(response, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_BODY);
            return;
        }
        chain.doFilter(request, response);
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(body);
    }
}
