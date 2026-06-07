package com.kraft.lotto.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kraft.lotto.feature.winningnumber.application.WinningStoreQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningRegionSummaryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningStoreDto;
import com.kraft.lotto.TestCacheConfig;
import com.kraft.lotto.support.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LottoDrawWinningStoreController.class)
@Import({GlobalExceptionHandler.class, TestCacheConfig.class})
@ImportAutoConfiguration(exclude = {OAuth2ClientWebSecurityAutoConfiguration.class})
@DisplayName("LottoDrawWinningStoreController")
class LottoDrawWinningStoreControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WinningStoreQueryService queryService;

    @Test
    @DisplayName("grade 지정 시 해당 등급 판매점 목록을 반환한다")
    void returnsStoresByGrade() throws Exception {
        WinningStoreDto dto = new WinningStoreDto(1, "행운복권방", "서울특별시 강남구 테헤란로 1", 1,
                "https://map.naver.com/v5/search/...", "서울", "강남구");
        when(queryService.findByRoundAndGrade(1227, 1)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/lotto/draws/1227/winning-stores").param("grade", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("행운복권방"))
                .andExpect(jsonPath("$[0].sido").value("서울"))
                .andExpect(jsonPath("$[0].sigungu").value("강남구"));
    }

    @Test
    @DisplayName("grade 미지정 시 1등·2등 모두 반환한다")
    void returnsAllGradesWhenNoGradeParam() throws Exception {
        WinningStoreDto dto1 = new WinningStoreDto(1, "1등복권방", "서울특별시 마포구 1", 1, "url", "서울", "마포구");
        WinningStoreDto dto2 = new WinningStoreDto(2, "2등복권방", "경기도 성남시 분당구 1", 1, "url", "경기", "성남시 분당구");
        when(queryService.findByRoundAndGrade(1227, 1)).thenReturn(List.of(dto1));
        when(queryService.findByRoundAndGrade(1227, 2)).thenReturn(List.of(dto2));

        mockMvc.perform(get("/api/lotto/draws/1227/winning-stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("데이터가 없는 회차는 빈 배열을 반환한다")
    void returnsEmptyListForUnknownRound() throws Exception {
        when(queryService.findByRoundAndGrade(9999, 1)).thenReturn(List.of());
        when(queryService.findByRoundAndGrade(9999, 2)).thenReturn(List.of());

        mockMvc.perform(get("/api/lotto/draws/9999/winning-stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("지역 집계를 반환한다")
    void returnsRegionSummary() throws Exception {
        WinningRegionSummaryDto region = new WinningRegionSummaryDto(1227, 1, "서울", "강남구", 2L);
        when(queryService.findRegionSummary(1227)).thenReturn(List.of(region));

        mockMvc.perform(get("/api/lotto/draws/1227/winning-regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sido").value("서울"))
                .andExpect(jsonPath("$[0].sigungu").value("강남구"))
                .andExpect(jsonPath("$[0].count").value(2));
    }
}
