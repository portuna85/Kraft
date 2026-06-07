package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("DhLotteryTracerClient")
class DhLotteryTracerClientTest {

    @Mock RestClient restClient;

    DhLotteryTracerClient tracer;
    DhLotteryTracerClient tracerSpy;

    @BeforeEach
    void setUp() {
        tracer     = new DhLotteryTracerClient(restClient, "1.2.3.4");
        tracerSpy  = spy(tracer);
    }

    // ── generateWcCookie ──────────────────────────────────────────

    @Test
    @DisplayName("wcCookie는 _T_5자리숫자_WC 형식이다")
    void generateWcCookie_matchesPattern() {
        assertThat(DhLotteryTracerClient.generateWcCookie()).matches("_T_\\d{5}_WC");
    }

    @Test
    @DisplayName("generateWcCookie 숫자는 10000~99999 범위이다")
    void generateWcCookie_numberInRange() {
        for (int i = 0; i < 20; i++) {
            String cookie = DhLotteryTracerClient.generateWcCookie();
            int num = Integer.parseInt(cookie.replace("_T_", "").replace("_WC", ""));
            assertThat(num).isBetween(10_000, 99_999);
        }
    }

    // ── checkBotIp ───────────────────────────────────────────────

    @Test
    @DisplayName("checkBotIp: HTTP 호출 실패 시 E를 반환한다")
    void checkBotIp_returnsE_onException() {
        when(restClient.post()).thenThrow(new RuntimeException("timeout"));

        assertThat(tracer.checkBotIp("/page")).isEqualTo("E");
    }

    // ── inputQueue ───────────────────────────────────────────────

    @Test
    @DisplayName("inputQueue: HTTP 호출 실패 시 E를 반환한다")
    void inputQueue_returnsE_onException() {
        when(restClient.post()).thenThrow(new RuntimeException("network error"));

        assertThat(tracer.inputQueue("/page", "login", "ua")).isEqualTo("E");
    }

    // ── pollQueue (spy로 inputQueue 스텁) ─────────────────────────

    @Test
    @DisplayName("pollQueue: inputQueue=F 이면 true를 반환한다")
    void pollQueue_returnsTrueWhenF() {
        doReturn("F").when(tracerSpy).inputQueue(anyString(), anyString(), anyString());

        assertThat(tracerSpy.pollQueue("/page", "login", "ua")).isTrue();
    }

    @Test
    @DisplayName("pollQueue: inputQueue=NE 이면 true를 반환한다")
    void pollQueue_returnsTrueWhenNE() {
        doReturn("NE").when(tracerSpy).inputQueue(anyString(), anyString(), anyString());

        assertThat(tracerSpy.pollQueue("/page", "login", "ua")).isTrue();
    }

    @Test
    @DisplayName("pollQueue: inputQueue=E 이면 true를 반환한다")
    void pollQueue_returnsTrueWhenE() {
        doReturn("E").when(tracerSpy).inputQueue(anyString(), anyString(), anyString());

        assertThat(tracerSpy.pollQueue("/page", "login", "ua")).isTrue();
    }

    @Test
    @DisplayName("pollQueue: inputQueue가 예상 외 응답(R 등)이면 진행(true)을 반환한다")
    void pollQueue_returnsTrueOnUnexpectedResponse() {
        doReturn("R").when(tracerSpy).inputQueue(anyString(), anyString(), anyString());

        assertThat(tracerSpy.pollQueue("/page", "login", "ua")).isTrue();
    }

    // ── performHandshake (spy로 하위 메서드 스텁) ─────────────────

    @Test
    @DisplayName("performHandshake: checkBotIp=E 이면 큐 없이 true를 반환한다")
    void performHandshake_trueWhenCheckBotIpReturnsE() {
        doReturn("E").when(tracerSpy).checkBotIp(anyString());

        assertThat(tracerSpy.performHandshake("/page", "login", "ua")).isTrue();
    }

    @Test
    @DisplayName("performHandshake: checkBotIp=F, pollQueue=true 이면 true를 반환한다")
    void performHandshake_trueWhenQueueClears() {
        doReturn("F").when(tracerSpy).checkBotIp(anyString());
        doReturn(true).when(tracerSpy).pollQueue(anyString(), anyString(), anyString());

        assertThat(tracerSpy.performHandshake("/page", "login", "ua")).isTrue();
    }

    @Test
    @DisplayName("performHandshake: checkBotIp=F, pollQueue=false 이면 false를 반환한다")
    void performHandshake_falseWhenQueueBlocked() {
        doReturn("F").when(tracerSpy).checkBotIp(anyString());
        doReturn(false).when(tracerSpy).pollQueue(anyString(), anyString(), anyString());

        assertThat(tracerSpy.performHandshake("/page", "login", "ua")).isFalse();
    }

    @Test
    @DisplayName("serverIp가 null이어도 동작한다")
    void constructor_acceptsNullServerIp() {
        DhLotteryTracerClient nullIpTracer = new DhLotteryTracerClient(restClient, null);
        DhLotteryTracerClient nullIpSpy    = spy(nullIpTracer);
        doReturn("E").when(nullIpSpy).checkBotIp(anyString());

        assertThat(nullIpSpy.performHandshake("/page", "login", "ua")).isTrue();
    }
}
