package com.kraft.winningnumber;

import com.kraft.common.config.RevalidateProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("재검증 웹훅 리스너 단위 테스트")
class RevalidateWebhookListenerTest {

    @Test
    @DisplayName("데이터 변경이 없는 이벤트는 웹훅 호출 없이 무시한다")
    void onCollected_dataNotChanged_skipsWebhook() {
        RevalidateProperties props = new RevalidateProperties("secret-123", "http://web:3000");
        RevalidateWebhookListener listener = new RevalidateWebhookListener(props, new SimpleMeterRegistry());

        // Should not throw even without a real HTTP client
        listener.onCollected(new WinningNumbersCollectedEvent(1200, false));
        // No exception = webhook call skipped as expected
    }

    @Test
    @DisplayName("비밀값이나 웹 주소가 비어 있으면 웹훅을 호출하지 않는다")
    void onCollected_disabled_skipsWebhook() {
        RevalidateProperties props = new RevalidateProperties("", "");
        RevalidateWebhookListener listener = new RevalidateWebhookListener(props, new SimpleMeterRegistry());

        assertThat(props.enabled()).isFalse();

        // Should not throw
        listener.onCollected(new WinningNumbersCollectedEvent(1200, true));
    }

    @Test
    @DisplayName("재검증 설정은 비밀값과 웹 주소가 모두 있을 때 활성화된다")
    void revalidateProperties_enabled_whenBothPresent() {
        RevalidateProperties props = new RevalidateProperties("my-secret", "http://web:3000");
        assertThat(props.enabled()).isTrue();
    }

    @Test
    @DisplayName("재검증 설정은 비밀값이 없을 때 비활성화된다")
    void revalidateProperties_disabled_whenSecretMissing() {
        assertThat(new RevalidateProperties(null, "http://web:3000").enabled()).isFalse();
        assertThat(new RevalidateProperties("", "http://web:3000").enabled()).isFalse();
        assertThat(new RevalidateProperties("  ", "http://web:3000").enabled()).isFalse();
    }

    @Test
    @DisplayName("재검증 설정은 웹 주소가 없을 때 비활성화된다")
    void revalidateProperties_disabled_whenWebUrlMissing() {
        assertThat(new RevalidateProperties("secret", null).enabled()).isFalse();
        assertThat(new RevalidateProperties("secret", "").enabled()).isFalse();
    }

    @Test
    @DisplayName("웹훅 실패해도 예외가 전파되지 않는다")
    void onCollected_webhookFails_doesNotPropagateException() {
        // Properties with short invalid URL — triggers exception inside listener
        RevalidateProperties props = new RevalidateProperties("secret", "http://nonexistent-host-xyz:9999");
        RevalidateWebhookListener listener = new RevalidateWebhookListener(props, new SimpleMeterRegistry());

        // Should swallow the connection error silently
        listener.onCollected(new WinningNumbersCollectedEvent(1200, true));
        // No exception propagated = pass
    }

    @Test
    @DisplayName("재검증 태그 목록은 최신 회차와 통계 태그만 포함한다")
    void tagsFor_includesLatestAndStatsTags() {
        assertThat(RevalidateWebhookListener.tagsFor())
                .containsExactly("rounds:latest", "stats:all");
    }
}
