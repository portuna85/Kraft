package com.kraft.lotto.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.TestCacheConfig;
import com.kraft.lotto.feature.recommend.application.RecommendService;
import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.support.GlobalExceptionHandler;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({HomeController.class, RecommendController.class})
@Import({GlobalExceptionHandler.class, TestCacheConfig.class, LottoBallHelper.class, RecommendModelSupport.class})
@ImportAutoConfiguration(exclude = {OAuth2ClientWebSecurityAutoConfiguration.class})
@DisplayName("홈 컨트롤러 WebMvc 슬라이스 테스트")
class HomeControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WinningNumberQueryService queryService;

    @MockitoBean
    RecommendService recommendService;

    @Test
    @DisplayName("예상되는 모델 속성과 함께 홈 뷰를 렌더링한다")
    void rendersHomeView() throws Exception {
        when(queryService.expectedCurrentRound()).thenReturn(1200);
        when(queryService.findLatest()).thenReturn(Optional.empty());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("expectedRound"));
    }

    @Test
    @DisplayName("숫자가 아닌 회차 파라미터에 대해 400을 반환한다")
    void invalidRoundTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/").param("round", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
    }

    @Test
    @DisplayName("oddCount가 허용 범위를 벗어나면 400을 반환한다")
    void invalidOddCountReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/fragments/recommend").param("oddCount", "7"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
    }
}
