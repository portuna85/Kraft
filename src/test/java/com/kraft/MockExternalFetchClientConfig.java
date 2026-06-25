package com.kraft;

import com.kraft.winningnumber.ExternalWinningNumberFetchClient;
import com.kraft.winningnumber.WinningNumberUpsertRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class MockExternalFetchClientConfig {

    @Bean
    @Primary
    ExternalWinningNumberFetchClient externalWinningNumberFetchClient() {
        return round -> new WinningNumberUpsertRequest(
                round,
                LocalDate.of(2026, 6, 20),
                List.of(5, 12, 18, 27, 36, 44),
                9,
                2_100_000_000L,
                null, null, null, null
        );
    }
}
