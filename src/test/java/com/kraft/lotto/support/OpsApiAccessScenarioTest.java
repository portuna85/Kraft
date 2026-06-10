package com.kraft.lotto.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.kraft.lotto.feature.news.application.NewsCollectionService;
import com.kraft.lotto.feature.recommend.application.RecommendMetricsQueryService;
import com.kraft.lotto.feature.winningnumber.application.ApiCircuitBreakerRegistry;
import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectStatusResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import com.kraft.lotto.infra.config.KraftSecurityProperties;
import com.kraft.lotto.web.OpsCollectionFacade;
import com.kraft.lotto.web.OpsCollectionController;
import com.kraft.lotto.web.OpsExceptionHandler;
import com.kraft.lotto.web.OpsFetchLogController;
import com.kraft.lotto.web.OpsMonitoringController;
import com.kraft.lotto.web.OpsNewsController;
import com.kraft.lotto.web.OpsNoStoreAdvice;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("운영 에이피아이 접근 제어 시나리오 테스트")
class OpsApiAccessScenarioTest {

    @Mock
    LottoFetchLogQueryService fetchLogQueryService;

    @Mock
    LottoCollectionCommandService collectionCommandService;

    @Mock
    RecommendMetricsQueryService recommendMetricsQueryService;

    @Mock
    ApiCircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    OpsCollectionFacade opsCollectionFacade;

    @Mock
    WinningNumberQueryService winningNumberQueryService;

    @Mock
    NewsCollectionService newsCollectionService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new OpsCollectionController(
                                collectionCommandService, opsCollectionFacade, securityProperties()),
                        new OpsFetchLogController(fetchLogQueryService, collectProperties()),
                        new OpsMonitoringController(
                                recommendMetricsQueryService,
                                circuitBreakerRegistry,
                                winningNumberQueryService,
                                Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneId.of("Asia/Seoul"))
                        ),
                        new OpsNewsController(newsCollectionService)
                )
                .setControllerAdvice(new OpsNoStoreAdvice(), new OpsExceptionHandler())
                .addFilters(new OpsAccessFilter(securityProperties()))
                .build();
    }

    @Test
    @DisplayName("운영 상태 조회 요청은 토큰이 없으면 401을 반환한다")
    void statusWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/ops/collect/status")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("운영 상태 조회 요청은 잘못된 토큰이면 401을 반환한다")
    void statusWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/ops/collect/status")
                        .header("X-Ops-Token", "wrong-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("운영 상태 조회 요청은 허용 목록 외 아이피이면 403을 반환한다")
    void statusFromDisallowedIpReturnsForbidden() throws Exception {
        mockMvc.perform(get("/ops/collect/status")
                        .header("X-Ops-Token", "expected-token")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.9");
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("운영 상태 조회 요청은 정상 토큰과 허용 아이피면 200을 반환한다")
    void statusWithValidTokenAndAllowedIpReturnsOk() throws Exception {
        when(collectionCommandService.getStatus())
                .thenReturn(new CollectStatusResponse(true, "collect-all", Instant.parse("2026-05-24T12:00:00Z"), 10));

        mockMvc.perform(get("/ops/collect/status")
                        .header("X-Ops-Token", "expected-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.operation").value("collect-all"));

        verify(collectionCommandService).getStatus();
    }

    @Test
    @DisplayName("운영 상태 조회 요청은 인증 전달자 토큰도 허용한다")
    void statusWithValidBearerTokenReturnsOk() throws Exception {
        when(collectionCommandService.getStatus())
                .thenReturn(new CollectStatusResponse(false, null, null, 0));

        mockMvc.perform(get("/ops/collect/status")
                        .header("Authorization", "Bearer expected-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0"))
                .andExpect(jsonPath("$.running").value(false));

        verify(collectionCommandService).getStatus();
    }

    @Test
    @DisplayName("수집 로그 보존 상태 조회 요청은 정상 토큰과 허용 아이피면 200을 반환한다")
    void retentionStatusWithValidTokenAndAllowedIpReturnsOk() throws Exception {
        when(fetchLogQueryService.retentionStatus(true, 90, 1000, "0 30 3 * * *", "Asia/Seoul"))
                .thenReturn(new FetchLogRetentionStatusDto(
                        LocalDateTime.of(2026, 5, 24, 21, 0),
                        true,
                        90,
                        1000,
                        "0 30 3 * * *",
                        "Asia/Seoul",
                        LocalDateTime.of(2026, 2, 23, 21, 0),
                        123L,
                        9L,
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 5, 24, 20, 59)
                ));

        mockMvc.perform(get("/ops/fetch-logs/retention-status")
                        .header("X-Ops-Token", "expected-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.retentionDays").value(90))
                .andExpect(jsonPath("$.purgeEligibleLogs").value(9));

        verify(fetchLogQueryService).retentionStatus(true, 90, 1000, "0 30 3 * * *", "Asia/Seoul");
    }

    @Test
    @DisplayName("운영 회로 차단기 조회 요청은 정상 토큰과 허용 아이피면 200을 반환한다")
    void circuitBreakersWithValidTokenAndAllowedIpReturnsOk() throws Exception {
        when(circuitBreakerRegistry.snapshots()).thenReturn(Map.of(
                "smok", new ApiCircuitBreakerRegistry.Snapshot(true, "closed")
        ));

        mockMvc.perform(get("/ops/circuit-breakers")
                        .header("X-Ops-Token", "expected-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients.smok.enabled").value(true))
                .andExpect(jsonPath("$.clients.smok.state").value("closed"));
    }

    @Test
    @DisplayName("수집 등록 요청은 정상 토큰과 허용 아이피면 퍼사드 위임 후 200을 반환한다")
    void collectWithValidTokenAndAllowedIpReturnsOk() throws Exception {
        when(opsCollectionFacade.collectLatest(any(), any())).thenReturn(
                com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse.of(
                        1, 0, 0, 1200, java.util.List.of(), false, null, false
                ));

        mockMvc.perform(post("/ops/collect")
                        .header("X-Ops-Token", "expected-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(1))
                .andExpect(jsonPath("$.latestRound").value(1200));

        verify(opsCollectionFacade).collectLatest(any(), any());
    }

    @Test
    @DisplayName("누락 회차 수집 등록 요청은 정상 토큰과 허용 아이피면 퍼사드 위임 후 200을 반환한다")
    void collectMissingWithValidTokenAndAllowedIpReturnsOk() throws Exception {
        when(opsCollectionFacade.collectMissing(any(), any())).thenReturn(
                com.kraft.lotto.feature.winningnumber.web.dto.CollectResponse.of(
                        2, 0, 0, 1200, java.util.List.of(), false, null, false
                ));

        mockMvc.perform(post("/ops/collect/missing")
                        .header("X-Ops-Token", "expected-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(2))
                .andExpect(jsonPath("$.latestRound").value(1200));

        verify(opsCollectionFacade).collectMissing(any(), any());
    }

    @Test
    @DisplayName("뉴스 수집 등록 요청은 정상 토큰과 허용 아이피면 뉴스 수집 결과를 반환한다")
    void collectNewsWithValidTokenAndAllowedIpReturnsOk() throws Exception {
        when(newsCollectionService.collect()).thenReturn(new NewsCollectionService.NewsCollectResult(3, 4));

        mockMvc.perform(post("/ops/news/collect")
                        .header("X-Ops-Token", "expected-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(3))
                .andExpect(jsonPath("$.skipped").value(4))
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0"));

        verify(newsCollectionService).collect();
        verify(newsCollectionService).purgeOldArticles();
    }

    @Test
    @DisplayName("데이터 최신성 조회 요청은 정상 토큰과 허용 아이피면 최신성 상태를 반환한다")
    void dataFreshnessWithValidTokenReturnsOk() throws Exception {
        when(winningNumberQueryService.findLatest()).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/ops/data-freshness")
                        .header("X-Ops-Token", "expected-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dbLatestRound").value(0))
                .andExpect(jsonPath("$.status").exists());
    }

    private static KraftSecurityProperties securityProperties() {
        KraftSecurityProperties props = new KraftSecurityProperties();
        props.getOps().setEnabled(true);
        props.getOps().setRequiredToken("expected-token");
        props.getOps().setAllowedIps(java.util.List.of("127.0.0.1"));
        return props;
    }

    private static KraftCollectProperties collectProperties() {
        return new KraftCollectProperties(
                52,
                2000,
                true,
                new KraftCollectProperties.Auto(true, "Asia/Seoul"),
                new KraftCollectProperties.LogRetention(true, 90, 1000, "0 30 3 * * *")
        );
    }
}
