package com.kraft.lotto.infra.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kraft.collect")
public record KraftCollectProperties(
        @Min(1) @Max(10_000) int maxPerRun,
        @Min(1) @Max(100_000) int maxHistoryCollect
) {
}

