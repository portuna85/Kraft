package com.kraft.lotto.feature.winningnumber.web.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record OpsCircuitBreakerStatusDto(
        LocalDateTime generatedAt,
        Map<String, CircuitBreakerState> clients
) {
    public record CircuitBreakerState(
            boolean enabled,
            String state
    ) {
    }
}

