package com.kraft.lotto.infra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kraft.api")
@Validated
public record KraftApiProperties(
        @NotBlank String client,
        @NotBlank String url,
        @Positive @Max(60_000) int connectTimeoutMs,
        @Positive @Max(60_000) int readTimeoutMs,
        @Positive @Max(60_000) int requestTimeoutMs,
        @PositiveOrZero @Max(10) int maxRetries,
        @PositiveOrZero @Max(60_000) int retryBackoffMs,
        @PositiveOrZero @Max(60_000) int backfillDelayMs,
        @PositiveOrZero @Max(168) int enrichDelayHours,
        @PositiveOrZero int mockLatestRound,
        boolean circuitBreakerEnabled,
        @Positive @Max(100) int circuitBreakerFailureThreshold,
        @Positive @Max(300_000) int circuitBreakerOpenDurationMs,
        @Positive @Max(10) int circuitBreakerHalfOpenMaxCalls,
        String userAgent,
        String referer,
        String acceptLanguage,
        String fallbackClient,
        String storeRelayUrl,
        String publicDataApiKey,
        String publicDataBaseUrl
) {
}
