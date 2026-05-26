package com.kraft.lotto.infra.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kraft.collect")
public record KraftCollectProperties(
        @Min(1) @Max(10_000) int maxPerRun,
        @Min(1) @Max(100_000) int maxHistoryCollect,
        @Valid Auto auto,
        @Valid LogRetention logRetention
) {

    public KraftCollectProperties {
        if (auto == null) {
            auto = new Auto(false, "Asia/Seoul");
        }
        if (logRetention == null) {
            logRetention = new LogRetention(true, 90, 1000, "0 30 3 * * *");
        }
    }

    public record Auto(
            boolean enabled,
            @NotBlank String zone
    ) {
        public Auto {
            zone = normalizeZone(zone);
        }
    }

    public record LogRetention(
            boolean enabled,
            @Min(1) @Max(3650) int days,
            @Min(100) @Max(100_000) int deleteBatchSize,
            @NotBlank String cron
    ) {
        public LogRetention {
            cron = Objects.requireNonNullElse(cron, "").trim();
            if (cron.isBlank()) {
                throw new IllegalArgumentException("kraft.collect.log-retention.cron must not be blank");
            }
        }
    }

    private static String normalizeZone(String zone) {
        String normalized = Objects.requireNonNullElse(zone, "").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("kraft.collect.auto.zone must not be blank");
        }
        return normalized;
    }
}
