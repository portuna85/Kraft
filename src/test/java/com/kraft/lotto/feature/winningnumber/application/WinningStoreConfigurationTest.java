package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.infra.config.KraftApiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WinningStoreConfiguration")
class WinningStoreConfigurationTest {

    @Test
    @DisplayName("storeRelayUrl이 설정되면 RelayStoreApiClient를 반환한다")
    void winningStoreApiClientReturnsRelayClientWhenRelayUrlSet() {
        WinningStoreConfiguration config = new WinningStoreConfiguration();
        KraftApiProperties properties = new KraftApiProperties(
                "testAgent", "http://localhost", 1000, 1000, 5000,
                0, 0, 0, 0, true, 5, 30_000, 1, null, null, null, null,
                "http://relay.example.com/stores"
        );

        WinningStoreApiClient client = config.winningStoreApiClient(properties, new ObjectMapper());

        assertThat(client).isInstanceOf(RelayStoreApiClient.class);
    }
}
