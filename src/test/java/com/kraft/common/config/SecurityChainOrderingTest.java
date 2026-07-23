package com.kraft.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kraft.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 커뮤니티 인증 체인(§7 1~2단계) 착수 전, 현재 보안 체인 순서(Admin=@Order(1),
 * Public=@Order(2))와 matcher 선점 상태를 고정하는 회귀 가드.
 *
 * 1단계에서 community 체인이 @Order(2)로 삽입되고 public 체인이 @Order(3)로
 * 재번호되면, communityPathIsNotYetClaimedByPublicChain()는 "community 경로가
 * community 체인에 도달"(예: 미인증 시 302 /oauth2/authorization/... 또는 401 JSON)을
 * 검증하도록 치환해야 한다.
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("보안 체인 순서·matcher 선점 회귀 가드")
class SecurityChainOrderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("미인증 admin 보호 경로는 admin 로그인 페이지로 리다이렉트된다")
    void unauthenticatedAdminPath_redirectsToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/login"));
    }

    @Test
    @DisplayName("공개 API 경로는 STATELESS로 처리되어 세션 쿠키가 생성되지 않는다")
    void publicApiPath_isStatelessAndSetsNoSessionCookie() throws Exception {
        mockMvc.perform(get("/api/v1/rounds/latest"))
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    @DisplayName("아직 community 체인이 없어 community 경로는 public 체인 matcher에 잡혀 404를 받는다")
    void communityPathIsNotYetClaimedByPublicChain() throws Exception {
        mockMvc.perform(get("/api/v1/community/ping"))
                .andExpect(status().isNotFound())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }
}
