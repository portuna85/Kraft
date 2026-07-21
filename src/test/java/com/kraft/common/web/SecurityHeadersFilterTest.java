package com.kraft.common.web;

import com.kraft.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("보안 헤더 필터 테스트")
class SecurityHeadersFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("일반 API 경로에는 보안 헤더가 모두 붙는다")
    void ordinaryApiPath_getsAllSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/stats/patterns"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "geolocation=(), microphone=(), camera=()"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
    }

    @Test
    @DisplayName("/admin 경로는 default-src 'none' CSP를 강제하지 않는다")
    void adminPath_doesNotForceDefaultCsp() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(result -> {
                    String csp = result.getResponse().getHeader("Content-Security-Policy");
                    if (csp != null && csp.contains("default-src 'none'")) {
                        throw new AssertionError("admin 경로에 API용 CSP가 적용됨: " + csp);
                    }
                });
    }

    @Test
    @DisplayName("/h2-console 경로는 default-src 'none' CSP를 강제하지 않는다")
    void h2ConsolePath_doesNotForceDefaultCsp() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(result -> {
                    String csp = result.getResponse().getHeader("Content-Security-Policy");
                    if (csp != null && csp.contains("default-src 'none'")) {
                        throw new AssertionError("h2-console 경로에 API용 CSP가 적용됨: " + csp);
                    }
                });
    }
}
