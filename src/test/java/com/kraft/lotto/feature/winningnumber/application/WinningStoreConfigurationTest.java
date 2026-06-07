package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.infra.config.KraftApiProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("WinningStoreConfiguration")
class WinningStoreConfigurationTest {

    @Test
    @DisplayName("storeRelayUrl이 설정되면 RelayStoreApiClient를 반환한다")
    void winningStoreApiClientReturnsRelayClientWhenRelayUrlSet() {
        WinningStoreConfiguration config = new WinningStoreConfiguration();
        KraftApiProperties properties = new KraftApiProperties(
                "testAgent", "http://localhost", 1000, 1000, 5000,
                0, 0, 0, 0, true, 5, 30_000, 1, null, null, null, null,
                "http://relay.example.com/stores", null, null
        );
        io.micrometer.core.instrument.MeterRegistry registry = new SimpleMeterRegistry();
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterProvider =
                new ObjectProvider<>() {
                    @Override public io.micrometer.core.instrument.MeterRegistry getObject() { return registry; }
                    @Override public io.micrometer.core.instrument.MeterRegistry getIfAvailable() { return registry; }
                    @Override public io.micrometer.core.instrument.MeterRegistry getIfUnique() { return registry; }
                    @Override public io.micrometer.core.instrument.MeterRegistry getObject(Object... args) { return registry; }
                };

        WinningStoreApiClient client = config.winningStoreApiClient(properties, new ObjectMapper(), meterProvider);

        assertThat(client).isInstanceOf(RelayStoreApiClient.class);
    }

    @Test
    @DisplayName("publicDataApiKey가 있으면 CompositeWinningStoreApiClient를 반환한다")
    void winningStoreApiClientReturnsCompositeWhenPublicDataKeySet() {
        WinningStoreConfiguration config = new WinningStoreConfiguration();
        KraftApiProperties properties = new KraftApiProperties(
                "testAgent", "http://localhost", 1000, 1000, 5000,
                0, 0, 0, 0, true, 5, 30_000, 1, null, null, null, null,
                null, "test-api-key", "https://apis.data.go.kr"
        );
        io.micrometer.core.instrument.MeterRegistry registry = new SimpleMeterRegistry();
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterProvider =
                new ObjectProvider<>() {
                    @Override public io.micrometer.core.instrument.MeterRegistry getObject() { return registry; }
                    @Override public io.micrometer.core.instrument.MeterRegistry getIfAvailable() { return registry; }
                    @Override public io.micrometer.core.instrument.MeterRegistry getIfUnique() { return registry; }
                    @Override public io.micrometer.core.instrument.MeterRegistry getObject(Object... args) { return registry; }
                };

        WinningStoreApiClient client = config.winningStoreApiClient(properties, new ObjectMapper(), meterProvider);

        assertThat(client).isInstanceOf(CompositeWinningStoreApiClient.class);
    }
}
