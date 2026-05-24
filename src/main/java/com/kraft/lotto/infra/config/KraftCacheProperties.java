package com.kraft.lotto.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kraft.cache")
public record KraftCacheProperties(
        @Valid Spec winningNumberFrequency,
        @Valid Spec combinationPrizeHistory,
        @Valid Spec winningFrequencySummary
) {
    public record Spec(
            @Positive int ttlMinutes,
            @Positive int maxSize
    ) {}
}
