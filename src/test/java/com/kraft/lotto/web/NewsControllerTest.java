package com.kraft.lotto.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.feature.news.application.NewsQueryService;
import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import com.kraft.lotto.feature.news.web.dto.NewsArticleDto;
import com.kraft.lotto.support.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("뉴스 컨트롤러")
class NewsControllerTest {

    @Mock
    NewsQueryService newsQueryService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NewsController(newsQueryService))
                .setViewResolvers(viewResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("/news 페이지를 렌더링한다")
    void newsPageRendersView() throws Exception {
        when(newsQueryService.list(anyInt(), anyInt(), any())).thenReturn(emptyPage());

        mockMvc.perform(get("/news"))
                .andExpect(status().isOk())
                .andExpect(view().name("news"))
                .andExpect(model().attributeExists("articles", "page", "size", "totalElements", "totalPages"));
    }

    @Test
    @DisplayName("page 파라미터가 숫자가 아니면 400을 반환한다")
    void nonNumericPageReturns400() throws Exception {
        mockMvc.perform(get("/news").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("page 기본값은 0이다")
    void defaultPageIsZero() throws Exception {
        when(newsQueryService.list(0, 20, null)).thenReturn(emptyPage());

        mockMvc.perform(get("/news"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("page", 0));
    }

    @Test
    @DisplayName("뉴스 목록이 있을 때 모델에 articles가 포함된다")
    void newsPageIncludesArticles() throws Exception {
        NewsArticleDto article = new NewsArticleDto(1L, "로또 뉴스", "https://example.com",
                "설명", "뉴스원", "2026.06.01 12:00", null);
        NewsQueryService.NewsPage page = new NewsQueryService.NewsPage(
                List.of(article), 0, 20, 1, 1);
        when(newsQueryService.list(0, 20, null)).thenReturn(page);

        mockMvc.perform(get("/news"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalElements", 1));
    }

    @Test
    @DisplayName("tier 파라미터가 있으면 등급 필터를 전달한다")
    void newsPagePassesTierFilter() throws Exception {
        when(newsQueryService.list(anyInt(), anyInt(), eq(NewsSourceTier.PRESS))).thenReturn(emptyPage());

        mockMvc.perform(get("/news").param("tier", "press"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentTier", "press"));

        verify(newsQueryService).list(0, 20, NewsSourceTier.PRESS);
    }

    @Test
    @DisplayName("잘못된 tier 파라미터는 400을 반환한다")
    void invalidTierReturns400() throws Exception {
        mockMvc.perform(get("/news").param("tier", "invalid"))
                .andExpect(status().isBadRequest());
    }

    private static NewsQueryService.NewsPage emptyPage() {
        return new NewsQueryService.NewsPage(List.of(), 0, 20, 0, 0);
    }
}
