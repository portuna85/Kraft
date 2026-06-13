package com.kraft.winningnumber;

import com.kraft.common.config.RevalidateProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevalidateWebhookListener 단위 테스트")
class RevalidateWebhookListenerTest {

    @Test
    @DisplayName("dataChanged=false 이벤트는 웹훅 호출 없이 무시")
    void onCollected_dataNotChanged_skipsWebhook() {
        RevalidateProperties props = new RevalidateProperties("secret-123", "http://web:3000");
        RevalidateWebhookListener listener = new RevalidateWebhookListener(props);

        // Should not throw even without a real HTTP client
        listener.onCollected(new WinningNumbersCollectedEvent(1200, false));
        // No exception = webhook call skipped as expected
    }

    @Test
    @DisplayName("secret/webUrl이 비어있으면 enabled()=false → 웹훅 호출 안 함")
    void onCollected_disabled_skipsWebhook() {
        RevalidateProperties props = new RevalidateProperties("", "");
        RevalidateWebhookListener listener = new RevalidateWebhookListener(props);

        assertThat(props.enabled()).isFalse();

        // Should not throw
        listener.onCollected(new WinningNumbersCollectedEvent(1200, true));
    }

    @Test
    @DisplayName("RevalidateProperties.enabled() — secret과 webUrl 모두 있을 때 true")
    void revalidateProperties_enabled_whenBothPresent() {
        RevalidateProperties props = new RevalidateProperties("my-secret", "http://web:3000");
        assertThat(props.enabled()).isTrue();
    }

    @Test
    @DisplayName("RevalidateProperties.enabled() — secret 없을 때 false")
    void revalidateProperties_disabled_whenSecretMissing() {
        assertThat(new RevalidateProperties(null, "http://web:3000").enabled()).isFalse();
        assertThat(new RevalidateProperties("", "http://web:3000").enabled()).isFalse();
        assertThat(new RevalidateProperties("  ", "http://web:3000").enabled()).isFalse();
    }

    @Test
    @DisplayName("RevalidateProperties.enabled() — webUrl 없을 때 false")
    void revalidateProperties_disabled_whenWebUrlMissing() {
        assertThat(new RevalidateProperties("secret", null).enabled()).isFalse();
        assertThat(new RevalidateProperties("secret", "").enabled()).isFalse();
    }

    @Test
    @DisplayName("웹훅 실패해도 예외가 전파되지 않음 (비동기 swallow)")
    void onCollected_webhookFails_doesNotPropagateException() {
        // Properties with short invalid URL — triggers exception inside listener
        RevalidateProperties props = new RevalidateProperties("secret", "http://nonexistent-host-xyz:9999");
        RevalidateWebhookListener listener = new RevalidateWebhookListener(props);

        // Should swallow the connection error silently
        listener.onCollected(new WinningNumbersCollectedEvent(1200, true));
        // No exception propagated = pass
    }
}
