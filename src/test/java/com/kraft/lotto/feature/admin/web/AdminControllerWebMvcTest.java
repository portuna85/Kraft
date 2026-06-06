package com.kraft.lotto.feature.admin.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.TestCacheConfig;
import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.admin.application.AdminNewsService;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreCollector;
import com.kraft.lotto.web.OpsCollectionFacade;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest({AdminLoginController.class, AdminCollectionController.class,
             AdminAuditController.class, AdminNewsController.class})
@Import({TestCacheConfig.class, AdminControllerWebMvcTest.SecurityMvcConfig.class})
@DisplayName("Admin 컨트롤러 WebMvc 테스트")
class AdminControllerWebMvcTest {

    @TestConfiguration
    static class SecurityMvcConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OpsCollectionFacade opsCollectionFacade;

    @MockitoBean
    WinningStoreCollector winningStoreCollector;

    @MockitoBean
    AdminAuditLogService auditLogService;

    @MockitoBean
    AdminNewsService adminNewsService;

    @Test
    @DisplayName("로그인 페이지 조회 — 인증 없이 200을 반환한다")
    void loginPageIsAccessible() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_OPERATOR")
    @DisplayName("운영 도구 페이지 조회 — ADMIN_OPERATOR 역할 사용자에게 200을 반환한다")
    void collectionPageIsOk() throws Exception {
        mockMvc.perform(get("/admin/ops/collection"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/collection"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_AUDITOR")
    @DisplayName("감사 로그 페이지 조회 — ADMIN_AUDITOR 역할 사용자에게 200을 반환한다")
    void auditPageIsOk() throws Exception {
        when(auditLogService.list(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/admin/ops/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_NEWS_MANAGER")
    @DisplayName("뉴스 관리 페이지 조회 — ADMIN_NEWS_MANAGER 역할 사용자에게 200을 반환한다")
    void newsPageIsOk() throws Exception {
        when(adminNewsService.listPending(any(Pageable.class))).thenReturn(Page.empty());
        mockMvc.perform(get("/admin/ops/news"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news"));
    }

    @Test
    @DisplayName("최신 회차 수집 실행 — 수집 후 운영 도구 페이지로 리다이렉트한다")
    void collectLatestRedirects() throws Exception {
        when(opsCollectionFacade.collectLatest(any(), anyString()))
                .thenReturn(com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse.ofInserted(3, 1200));

        mockMvc.perform(post("/admin/ops/collection/latest")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN_OPERATOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ops/collection"));
    }

    @Test
    @DisplayName("판매점 정보 수집 실행 — 수집 후 운영 도구 페이지로 리다이렉트한다")
    void collectStoresRedirects() throws Exception {
        when(winningStoreCollector.collectStores(anyInt())).thenReturn(true);

        mockMvc.perform(post("/admin/ops/collection/stores")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN_OPERATOR"))
                        .param("round", "1200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ops/collection"));
    }

    @Test
    @DisplayName("뉴스 승인 처리 — 승인 후 뉴스 관리 페이지로 리다이렉트한다")
    void approveRedirects() throws Exception {
        doNothing().when(adminNewsService).approve(anyLong(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/admin/ops/news/42/approve")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN_NEWS_MANAGER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ops/news"));
    }

    @Test
    @DisplayName("뉴스 거절 처리 — 거절 후 뉴스 관리 페이지로 리다이렉트한다")
    void rejectRedirects() throws Exception {
        doNothing().when(adminNewsService).reject(anyLong(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/admin/ops/news/42/reject")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN_NEWS_MANAGER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ops/news"));
    }

    @Test
    @DisplayName("도메인 차단 처리 — 차단 후 뉴스 관리 페이지로 리다이렉트한다")
    void blockDomainRedirects() throws Exception {
        doNothing().when(adminNewsService).blockDomain(anyLong(), anyString(), anyString(),
                anyString(), anyString());

        mockMvc.perform(post("/admin/ops/news/42/block-domain")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN_NEWS_MANAGER"))
                        .param("reason", "스팸"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ops/news"));
    }

    @Test
    @DisplayName("키워드 차단 처리 — 차단 후 뉴스 관리 페이지로 리다이렉트한다")
    void blockKeywordRedirects() throws Exception {
        doNothing().when(adminNewsService).blockKeyword(anyString(), anyString(), anyString(),
                anyString(), anyString());

        mockMvc.perform(post("/admin/ops/news/block-keyword")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN_NEWS_MANAGER"))
                        .param("keyword", "파워볼 로또"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ops/news"));
    }
}
