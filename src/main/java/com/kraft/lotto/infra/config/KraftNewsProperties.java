package com.kraft.lotto.infra.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kraft.news")
@Validated
public record KraftNewsProperties(
        boolean enabled,
        @NotBlank String cron,
        @NotBlank String rssUrl,
        @Min(1) @Max(100) int maxArticlesPerRun,
        @Min(1) @Max(365) int retentionDays,
        @Positive @Max(60_000) int connectTimeoutMs,
        @Positive @Max(60_000) int readTimeoutMs,
        List<String> excludeKeywords,
        List<String> officialSources,
        List<String> pressSources
) {
    public KraftNewsProperties {
        excludeKeywords  = excludeKeywords  == null ? List.of() : List.copyOf(excludeKeywords);
        officialSources  = officialSources  == null ? List.of() : List.copyOf(officialSources);
        pressSources     = pressSources     == null ? List.of() : List.copyOf(pressSources);
    }
}
