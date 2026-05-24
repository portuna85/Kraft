package com.kraft.lotto.feature.winningnumber.web.dto;

import java.time.Instant;

public record CollectStatusResponse(boolean running, String operation, Instant startedAt, long elapsedSeconds) {

    public static CollectStatusResponse idle() {
        return new CollectStatusResponse(false, null, null, 0L);
    }

    public static CollectStatusResponse active(String operation, Instant startedAt) {
        long elapsed = startedAt != null
                ? java.time.Duration.between(startedAt, Instant.now()).getSeconds()
                : 0L;
        return new CollectStatusResponse(true, operation, startedAt, elapsed);
    }
}
