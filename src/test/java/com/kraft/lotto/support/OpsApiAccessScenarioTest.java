package com.kraft.lotto.support;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.kraft.lotto.feature.recommend.application.RecommendMetricsQueryService;
import com.kraft.lotto.feature.winningnumber.application.ApiCircuitBreakerRegistry;
import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.CollectStatusResponse;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import com.kraft.lotto.infra.config.KraftSecurityProperties;
import com.kraft.lotto.web.OpsCollectionFacade;
import com.kraft.lotto.web.OpsController;
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
@DisplayName("운영 API 접근 제어 시나리오 테스트")
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

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OpsController controller = new OpsController(
                fetchLogQueryService,
                collectionCommandService,
                opsCollectionFacade,
                recommendMetricsQueryService,
                circuitBreakerRegistry,
                collectProperties(),
                Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new OpsAccessFilter(securityProperties()))
                .build();
    }

    @Test
    @DisplayName("GET /ops/collect/status: 토큰이 없으면 401")
    void statusWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/ops/collect/status")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /ops/collect/status: 잘못된 토큰이면 401")
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
    @DisplayName("GET /ops/collect/status: allowlist 외 IP는 403")
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
    @DisplayName("GET /ops/collect/status: 정상 토큰/허용 IP면 200")
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
    @DisplayName("GET /ops/collect/status: Authorization Bearer 토큰도 허용")
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
    @DisplayName("GET /ops/fetch-logs/retention-status: 정상 토큰/허용 IP면 200")
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
    @DisplayName("GET /ops/circuit-breakers: 정상 토큰/허용 IP면 200")
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
    @DisplayName("POST /ops/collect: 정상 토큰/허용 IP면 facade 위임 후 200")
    void collectWithValidTokenAndAllowedIpReturnsOk() throws Exception {
        when(opsCollectionFacade.collectLatest()).thenReturn(
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

        verify(opsCollectionFacade).collectLatest();
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
